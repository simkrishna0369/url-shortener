package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortUrlId(Long shortUrlId);

    List<ClickEvent> findByShortUrlIdOrderByClickedAtDesc(Long shortUrlId, Pageable pageable);

    @Query("""
            SELECT COALESCE(e.deviceType, 'unknown') AS groupingKey, COUNT(e) AS total
            FROM ClickEvent e
            WHERE e.shortUrl.id = :shortUrlId
            GROUP BY COALESCE(e.deviceType, 'unknown')
            """)
    List<KeyCount> countGroupedByDevice(@Param("shortUrlId") Long shortUrlId);

    @Query("""
            SELECT CASE
                WHEN e.referrer IS NULL OR TRIM(e.referrer) = '' THEN 'direct'
                ELSE e.referrer
            END AS groupingKey, COUNT(e) AS total
            FROM ClickEvent e
            WHERE e.shortUrl.id = :shortUrlId
            GROUP BY CASE
                WHEN e.referrer IS NULL OR TRIM(e.referrer) = '' THEN 'direct'
                ELSE e.referrer
            END
            """)
    List<KeyCount> countGroupedByReferrer(@Param("shortUrlId") Long shortUrlId);

    interface KeyCount {
        String getGroupingKey();

        long getTotal();
    }
}
