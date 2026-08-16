package com.urlshortener.dto;

import java.time.Instant;

public record CreateShortUrlResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        Instant createdAt
) {
}
