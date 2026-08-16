package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateShortUrlRequest {

    @NotBlank(message = "url is required")
    @Size(max = 2048, message = "url exceeds maximum length")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
