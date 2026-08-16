package com.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_createRedirectAndTrackAnalytics_when_urlIsValid() throws Exception {
        String shortCode = createUrl("https://example.com/integration");

        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longUrl").value("https://example.com/integration"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/" + shortCode)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)")
                        .header("Referer", "https://news.example"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/integration"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.clickCount").value(1))
                        .andExpect(jsonPath("$.clicks[0].deviceType").value("mobile"))
                        .andExpect(jsonPath("$.clicks[0].referrer").value("https://news.example"))
                        .andExpect(jsonPath("$.breakdown.devices.mobile").value(1)));
    }

    @Test
    void should_rejectUnsafeUrl_when_schemeIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_URL"));
    }

    @Test
    void should_return404_when_shortCodeMissing() throws Exception {
        mockMvc.perform(get("/zzzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void should_return404OnRedirect_when_cachedCodeIsDeactivated() throws Exception {
        String shortCode = createUrl("https://example.com/cached-then-deleted");

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/cached-then-deleted"));
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/cached-then-deleted"));

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void should_return404_when_deactivatingUnknownShortCode() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/zzzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void should_return204_when_deactivatingAlreadyInactiveLink() throws Exception {
        String shortCode = createUrl("https://example.com/delete-twice");

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_keepAnalytics_when_linkIsSoftDeleted() throws Exception {
        String shortCode = createUrl("https://example.com/to-delete");

        mockMvc.perform(get("/" + shortCode)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
                .andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                        .andExpect(jsonPath("$.clickCount").value(1)));

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void should_notBreakRedirect_when_userAgentHeaderIsMissing() throws Exception {
        String shortCode = createUrl("https://example.com/no-ua");

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/no-ua"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.clickCount").value(1))
                        .andExpect(jsonPath("$.clicks[0].deviceType").value("unknown")));
    }

    @Test
    void should_incrementClickCount_when_redirectedMultipleTimes() throws Exception {
        String shortCode = createUrl("https://example.com/multi");

        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/urls/" + shortCode + "/analytics"))
                        .andExpect(jsonPath("$.clickCount").value(3)));
    }

    @Test
    void should_returnServiceIndex_when_rootPathRequested() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("url-shortener"));
    }

    @Test
    void should_return404_when_unknownPathRequested() throws Exception {
        mockMvc.perform(get("/this-is-not-an-api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private String createUrl(String longUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + longUrl + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.get("shortCode").asText()).hasSize(7);
        return body.get("shortCode").asText();
    }
}
