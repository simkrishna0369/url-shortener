package com.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl = "http://localhost:8080";
    private int shortCodeLength = 7;
    private final Url url = new Url();
    private final Cache cache = new Cache();
    private final RateLimit rateLimit = new RateLimit();
    private final Async async = new Async();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getShortCodeLength() {
        return shortCodeLength;
    }

    public void setShortCodeLength(int shortCodeLength) {
        this.shortCodeLength = shortCodeLength;
    }

    public Url getUrl() {
        return url;
    }

    public Cache getCache() {
        return cache;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Async getAsync() {
        return async;
    }

    public static class Url {
        private int maxLength = 2048;
        private String allowedSchemes = "http,https";

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getAllowedSchemes() {
            return allowedSchemes;
        }

        public void setAllowedSchemes(String allowedSchemes) {
            this.allowedSchemes = allowedSchemes;
        }
    }

    public static class Cache {
        private long ttlSeconds = 300;
        private long maxSize = 10_000;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }
    }

    public static class RateLimit {
        private int capacity = 30;
        private int refillTokens = 30;
        private long refillPeriodSeconds = 60;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(long refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }

    public static class Async {
        private int clickCorePoolSize = 2;
        private int clickMaxPoolSize = 8;
        private int clickQueueCapacity = 1000;

        public int getClickCorePoolSize() {
            return clickCorePoolSize;
        }

        public void setClickCorePoolSize(int clickCorePoolSize) {
            this.clickCorePoolSize = clickCorePoolSize;
        }

        public int getClickMaxPoolSize() {
            return clickMaxPoolSize;
        }

        public void setClickMaxPoolSize(int clickMaxPoolSize) {
            this.clickMaxPoolSize = clickMaxPoolSize;
        }

        public int getClickQueueCapacity() {
            return clickQueueCapacity;
        }

        public void setClickQueueCapacity(int clickQueueCapacity) {
            this.clickQueueCapacity = clickQueueCapacity;
        }
    }
}
