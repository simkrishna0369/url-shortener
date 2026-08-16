package com.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAgentParserTest {

    @Test
    void should_returnUnknown_when_userAgentMissing() {
        assertThat(UserAgentParser.deviceType(null)).isEqualTo("unknown");
        assertThat(UserAgentParser.deviceType(" ")).isEqualTo("unknown");
    }

    @Test
    void should_classifyMobileDesktopAndBot() {
        assertThat(UserAgentParser.deviceType("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)")).isEqualTo("mobile");
        assertThat(UserAgentParser.deviceType("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")).isEqualTo("desktop");
        assertThat(UserAgentParser.deviceType("Googlebot/2.1")).isEqualTo("bot");
        assertThat(UserAgentParser.deviceType("Mozilla/5.0 (iPad; CPU OS 16_0)")).isEqualTo("tablet");
    }
}
