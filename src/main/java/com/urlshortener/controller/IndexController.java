package com.urlshortener.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "service", "url-shortener",
                "hint", "This is an API, not a website. POST a JSON body to create a short URL.",
                "endpoints", List.of(
                        "POST /api/v1/urls",
                        "GET /{shortCode}",
                        "GET /api/v1/urls/{shortCode}",
                        "GET /api/v1/urls/{shortCode}/analytics",
                        "DELETE /api/v1/urls/{shortCode}"));
    }
}
