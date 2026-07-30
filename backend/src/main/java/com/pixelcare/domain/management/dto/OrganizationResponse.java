package com.pixelcare.domain.management.dto;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String organizationType,
        String registrationNumber,
        String representativeName,
        String phone,
        String email,
        String address,
        String description,
        String homepageUrl,
        boolean canIssueDonationReceipt,
        String verificationStatus,
        LocalDateTime createdAt
) {}
