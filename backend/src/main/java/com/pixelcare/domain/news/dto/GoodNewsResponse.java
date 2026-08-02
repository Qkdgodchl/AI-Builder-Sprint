package com.pixelcare.domain.news.dto;

import java.time.Instant;
import java.util.List;

public record GoodNewsResponse(
        String region,
        List<GoodNewsItem> items,
        Instant updatedAt,
        String provider,
        boolean stale,
        String message
) {}
