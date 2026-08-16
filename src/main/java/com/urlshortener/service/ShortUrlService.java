package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.config.CacheConfig;
import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateShortUrlResponse;
import com.urlshortener.dto.RedirectTarget;
import com.urlshortener.dto.ShortUrlMetadataResponse;
import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.ShortUrl;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.Base62Encoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final UrlValidationService urlValidationService;
    private final AppProperties properties;

    public ShortUrlService(
            ShortUrlRepository shortUrlRepository,
            ClickEventRepository clickEventRepository,
            UrlValidationService urlValidationService,
            AppProperties properties) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.urlValidationService = urlValidationService;
        this.properties = properties;
    }

    @Transactional
    public CreateShortUrlResponse create(String rawUrl) {
        String longUrl = urlValidationService.validateAndNormalize(rawUrl);
        ShortUrl entity = new ShortUrl();
        entity.setLongUrl(longUrl);
        entity.setActive(true);
        ShortUrl saved = shortUrlRepository.save(entity);
        saved.setShortCode(Base62Encoder.encode(saved.getId(), properties.getShortCodeLength()));
        ShortUrl persisted = shortUrlRepository.save(saved);

        MDC.put("shortCode", persisted.getShortCode());
        try {
            log.info("Created short URL");
        } finally {
            MDC.remove("shortCode");
        }

        return new CreateShortUrlResponse(
                persisted.getShortCode(),
                properties.getBaseUrl() + "/" + persisted.getShortCode(),
                persisted.getLongUrl(),
                persisted.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ShortUrlMetadataResponse getMetadata(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        return new ShortUrlMetadataResponse(
                entity.getShortCode(),
                entity.getLongUrl(),
                entity.getCreatedAt(),
                entity.isActive());
    }

    @Cacheable(cacheNames = CacheConfig.SHORT_URL_CACHE, key = "#shortCode")
    @Transactional(readOnly = true)
    public RedirectTarget requireActive(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        return new RedirectTarget(entity.getId(), entity.getShortCode(), entity.getLongUrl());
    }

    @CacheEvict(cacheNames = CacheConfig.SHORT_URL_CACHE, key = "#shortCode")
    @Transactional
    public void deactivate(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        entity.setActive(false);
        shortUrlRepository.save(entity);
        MDC.put("shortCode", shortCode);
        try {
            log.info("Deactivated short URL");
        } finally {
            MDC.remove("shortCode");
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl entity = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        Long shortUrlId = entity.getId();
        long clickCount = clickEventRepository.countByShortUrlId(shortUrlId);
        int clicksLimit = Math.max(1, properties.getAnalytics().getClicksLimit());

        List<ClickEvent> newestFirst = clickEventRepository.findByShortUrlIdOrderByClickedAtDesc(
                shortUrlId, PageRequest.of(0, clicksLimit));
        List<ClickEvent> window = new ArrayList<>(newestFirst);
        Collections.reverse(window);

        List<AnalyticsResponse.ClickDto> clicks = window.stream()
                .map(event -> new AnalyticsResponse.ClickDto(
                        event.getClickedAt(),
                        event.getDeviceType(),
                        event.getReferrer()))
                .toList();

        Map<String, Long> devices = toCountMap(clickEventRepository.countGroupedByDevice(shortUrlId));
        Map<String, Long> referrers = toCountMap(clickEventRepository.countGroupedByReferrer(shortUrlId));

        return new AnalyticsResponse(
                entity.getShortCode(),
                clickCount,
                clicks,
                clickCount > clicks.size(),
                new AnalyticsResponse.Breakdown(devices, referrers));
    }

    private static Map<String, Long> toCountMap(List<ClickEventRepository.KeyCount> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClickEventRepository.KeyCount row : rows) {
            counts.put(row.getGroupingKey(), row.getTotal());
        }
        return counts;
    }
}
