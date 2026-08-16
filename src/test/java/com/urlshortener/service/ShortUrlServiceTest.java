package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.CreateShortUrlResponse;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private ClickEventRepository clickEventRepository;

    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setBaseUrl("http://localhost:8080");
        properties.setShortCodeLength(7);
        UrlValidationService validationService = new UrlValidationService(properties);
        service = new ShortUrlService(shortUrlRepository, clickEventRepository, validationService, properties);
    }

    @Test
    void should_assignBase62ShortCode_when_creatingUrl() {
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer((Answer<ShortUrl>) invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                java.lang.reflect.Field id = ShortUrl.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(entity, 1L);
            }
            return entity;
        });

        CreateShortUrlResponse response = service.create("https://example.com/docs");

        assertThat(response.shortCode()).isEqualTo("0000001");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/0000001");
        assertThat(response.longUrl()).isEqualTo("https://example.com/docs");
    }
}
