package com.pixelcare.domain.stats.dto;

import java.math.BigDecimal;

/** 홈 상단 누적 현황에 노출하는 플랫폼 전체 집계. */
public record PlatformStatsResponse(
        double averageWarmth,
        BigDecimal pledgedDonation,
        long volunteerHours,
        long signedCommitments,
        long activeMembers
) {}
