package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.exception.InvalidUrlException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UrlValidationService {

    private final AppProperties properties;

    public UrlValidationService(AppProperties properties) {
        this.properties = properties;
    }

    public String validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("url is required");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.length() > properties.getUrl().getMaxLength()) {
            throw new InvalidUrlException("url exceeds maximum length of " + properties.getUrl().getMaxLength());
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("url is malformed");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new InvalidUrlException("url must include a scheme (http or https)");
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        Set<String> allowed = Arrays.stream(properties.getUrl().getAllowedSchemes().split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        if (!allowed.contains(normalizedScheme)) {
            throw new InvalidUrlException("url scheme is not allowed");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("url must include a host");
        }
        return trimmed;
    }
}
