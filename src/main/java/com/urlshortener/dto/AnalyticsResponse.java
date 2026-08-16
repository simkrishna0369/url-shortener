package com.urlshortener.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        String shortCode,
        long clickCount,
        List<ClickDto> clicks,
        Breakdown breakdown
) {

    public record ClickDto(Instant timestamp, String deviceType, String referrer) {
    }

    public record Breakdown(Map<String, Long> devices, Map<String, Long> referrers) {
    }
}
