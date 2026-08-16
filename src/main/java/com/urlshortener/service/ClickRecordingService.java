package com.urlshortener.service;

import com.urlshortener.config.AsyncConfig;
import com.urlshortener.entity.ClickEvent;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.util.UserAgentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickRecordingService {

    private static final Logger log = LoggerFactory.getLogger(ClickRecordingService.class);

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;

    public ClickRecordingService(
            ClickEventRepository clickEventRepository,
            ShortUrlRepository shortUrlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.shortUrlRepository = shortUrlRepository;
    }

    @Async(AsyncConfig.CLICK_EXECUTOR)
    @Transactional
    public void recordClick(Long shortUrlId, String shortCode, String userAgent, String referrer) {
        try {
            MDC.put("shortCode", shortCode);
            ClickEvent event = new ClickEvent();
            event.setShortUrl(shortUrlRepository.getReferenceById(shortUrlId));
            event.setUserAgent(truncate(userAgent, 512));
            event.setReferrer(truncate(referrer, 2048));
            event.setDeviceType(UserAgentParser.deviceType(userAgent));
            clickEventRepository.save(event);
            log.info("Recorded click for short URL");
        } catch (Exception ex) {
            log.error("Failed to record click; redirect already completed", ex);
        } finally {
            MDC.remove("shortCode");
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
