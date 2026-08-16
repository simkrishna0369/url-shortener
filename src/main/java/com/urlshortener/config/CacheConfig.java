package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    public static final String SHORT_URL_CACHE = "shortUrls";

    @Bean
    public CacheManager cacheManager(AppProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(SHORT_URL_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getCache().getTtlSeconds()))
                .maximumSize(properties.getCache().getMaxSize()));
        return cacheManager;
    }
}
