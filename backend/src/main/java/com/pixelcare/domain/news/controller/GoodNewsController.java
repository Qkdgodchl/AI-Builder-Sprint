package com.pixelcare.domain.news.controller;

import com.pixelcare.domain.news.dto.GoodNewsResponse;
import com.pixelcare.domain.news.service.GoodNewsService;
import com.pixelcare.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/good-news")
public class GoodNewsController {

    private final GoodNewsService service;

    public GoodNewsController(GoodNewsService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<GoodNewsResponse> list(
            @RequestParam(defaultValue = "전국") String region,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ApiResponse.success(service.getNews(region, limit));
    }
}
