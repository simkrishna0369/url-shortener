package com.urlshortener.util;

public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String deviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
            return "bot";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "mobile";
        }
        return "desktop";
    }
}
