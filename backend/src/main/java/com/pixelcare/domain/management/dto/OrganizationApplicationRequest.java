package com.pixelcare.domain.management.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationApplicationRequest(
        @NotBlank(message = "센터명은 필수입니다.")
        String name,
        @NotBlank(message = "기관 유형은 필수입니다.")
        String organizationType,
        String registrationNumber,
        @NotBlank(message = "대표자명은 필수입니다.")
        String representativeName,
        String phone,
        String email,
        String address,
        String description,
        Long evidenceFileId
) {}
