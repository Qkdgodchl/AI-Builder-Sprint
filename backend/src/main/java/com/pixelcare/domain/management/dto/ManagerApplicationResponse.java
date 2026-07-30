package com.pixelcare.domain.management.dto;

import java.time.LocalDateTime;

public record ManagerApplicationResponse(
        String publicId,
        Long applicantUserId,
        String applicantEmail,
        String organizationName,
        String position,
        String contact,
        String organizationType,
        String registrationNumber,
        Long evidenceFileId,
        String reason,
        String plannedCenterName,
        String status,
        String reviewReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}
