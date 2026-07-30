package com.pixelcare.domain.management.dto;

import java.time.LocalDateTime;

public record OrganizationApplicationResponse(
        String publicId,
        Long applicantUserId,
        String applicantEmail,
        String name,
        String organizationType,
        String registrationNumber,
        String representativeName,
        String phone,
        String email,
        String address,
        String description,
        Long evidenceFileId,
        String status,
        String reviewReason,
        Long createdOrganizationId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}
