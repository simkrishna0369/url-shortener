package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateShortUrlRequest;
import com.urlshortener.dto.CreateShortUrlResponse;
import com.urlshortener.dto.ShortUrlMetadataResponse;
import com.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final ShortUrlService shortUrlService;

    public UrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(request.getUrl()));
    }

    @GetMapping("/{shortCode}")
    public ShortUrlMetadataResponse metadata(@PathVariable String shortCode) {
        return shortUrlService.getMetadata(shortCode);
    }

    @GetMapping("/{shortCode}/analytics")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        return shortUrlService.getAnalytics(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deactivate(@PathVariable String shortCode) {
        shortUrlService.deactivate(shortCode);
        return ResponseEntity.noContent().build();
    }
}
