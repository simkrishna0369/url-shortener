package com.urlshortener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String CREATE_PATH = "/api/v1/urls";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(isCreateEndpoint(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String clientIp = clientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, ignored -> new TokenBucket(properties.getRateLimit()));
        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for create endpoint");
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = new ErrorResponse(
                    "RATE_LIMITED",
                    "Too many URL create requests. Retry later.",
                    Instant.now());
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isCreateEndpoint(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && CREATE_PATH.equals(request.getRequestURI());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    static final class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final long refillPeriodNanos;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(AppProperties.RateLimit config) {
            this.capacity = config.getCapacity();
            this.refillTokens = config.getRefillTokens();
            this.refillPeriodNanos = config.getRefillPeriodSeconds() * 1_000_000_000L;
            this.tokens = config.getCapacity();
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            if (elapsed <= 0 || refillPeriodNanos <= 0) {
                return;
            }
            double added = (elapsed / (double) refillPeriodNanos) * refillTokens;
            if (added > 0) {
                tokens = Math.min(capacity, tokens + added);
                lastRefillNanos = now;
            }
        }
    }
}
