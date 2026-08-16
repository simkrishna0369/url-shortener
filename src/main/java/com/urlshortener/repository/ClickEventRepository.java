package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlIdOrderByClickedAtAsc(Long shortUrlId);

    long countByShortUrlId(Long shortUrlId);
}
