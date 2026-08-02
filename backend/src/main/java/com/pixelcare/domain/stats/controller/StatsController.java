package com.pixelcare.domain.stats.controller;

import com.pixelcare.domain.stats.dto.PlatformStatsResponse;
import com.pixelcare.domain.stats.repository.StatsRepository;
import com.pixelcare.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 홈 화면에서 조회하는 공개 통계. */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsRepository statsRepository;

    public StatsController(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @GetMapping("/summary")
    public ApiResponse<PlatformStatsResponse> summary() {
        return ApiResponse.success(statsRepository.summary());
    }
}
