package com.urlshortener.controller;

import com.urlshortener.dto.RedirectTarget;
import com.urlshortener.service.ClickRecordingService;
import com.urlshortener.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final ShortUrlService shortUrlService;
    private final ClickRecordingService clickRecordingService;

    public RedirectController(ShortUrlService shortUrlService, ClickRecordingService clickRecordingService) {
        this.shortUrlService = shortUrlService;
        this.clickRecordingService = clickRecordingService;
    }

    @GetMapping("/{shortCode:[0-9A-Za-z]{7}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        MDC.put("shortCode", shortCode);
        try {
            RedirectTarget target = shortUrlService.requireActive(shortCode);
            clickRecordingService.recordClick(
                    target.id(),
                    target.shortCode(),
                    request.getHeader("User-Agent"),
                    request.getHeader("Referer"));
            log.info("Redirecting short URL");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(target.longUrl()))
                    .build();
        } finally {
            MDC.remove("shortCode");
        }
    }
}
