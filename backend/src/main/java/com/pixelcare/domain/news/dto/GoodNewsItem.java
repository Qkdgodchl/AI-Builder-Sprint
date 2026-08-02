package com.pixelcare.domain.news.dto;

public record GoodNewsItem(
        String id,
        String region,
        String title,
        String summary,
        String source,
        String url,
        String publishedAt
) {}
