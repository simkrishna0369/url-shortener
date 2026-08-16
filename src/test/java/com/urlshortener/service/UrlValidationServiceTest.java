package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urlshortener.config.AppProperties;
import com.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UrlValidationServiceTest {

    private UrlValidationService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getUrl().setMaxLength(2048);
        properties.getUrl().setAllowedSchemes("http,https");
        service = new UrlValidationService(properties);
    }

    @Test
    void should_acceptHttpAndHttpsUrls() {
        assertThat(service.validateAndNormalize("https://example.com/path")).isEqualTo("https://example.com/path");
        assertThat(service.validateAndNormalize(" http://example.com ")).isEqualTo("http://example.com");
    }

    @Test
    void should_rejectUnsafeSchemes() {
        assertThatThrownBy(() -> service.validateAndNormalize("javascript:alert(1)"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.validateAndNormalize("data:text/html,hi"))
                .isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.validateAndNormalize("file:///etc/passwd"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void should_rejectMissingHost() {
        assertThatThrownBy(() -> service.validateAndNormalize("https://"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void should_rejectTooLongUrls() {
        AppProperties properties = new AppProperties();
        properties.getUrl().setMaxLength(20);
        UrlValidationService tight = new UrlValidationService(properties);
        assertThatThrownBy(() -> tight.validateAndNormalize("https://example.com/this-is-too-long"))
                .isInstanceOf(InvalidUrlException.class);
    }
}
