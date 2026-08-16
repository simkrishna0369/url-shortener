package com.urlshortener.dto;

import java.time.Instant;

public record ShortUrlMetadataResponse(
        String shortCode,
        String longUrl,
        Instant createdAt,
        boolean active
) {
}
