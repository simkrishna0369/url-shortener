package com.urlshortener.dto;

public record RedirectTarget(Long id, String shortCode, String longUrl) {
}
