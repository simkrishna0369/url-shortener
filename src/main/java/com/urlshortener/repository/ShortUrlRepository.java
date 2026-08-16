package com.urlshortener.repository;

import com.urlshortener.entity.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    Optional<ShortUrl> findByShortCodeAndActiveTrue(String shortCode);
}
