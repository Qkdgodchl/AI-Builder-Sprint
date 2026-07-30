package com.pixelcare.domain.management.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ManagerApplicationRequest(
        @NotBlank(message = "기관명은 필수입니다.")
        String organizationName,
        @NotBlank(message = "직책은 필수입니다.")
        String position,
        @NotBlank(message = "연락처는 필수입니다.")
        String contact,
        @NotBlank(message = "기관 유형은 필수입니다.")
        String organizationType,
        String registrationNumber,
        Long evidenceFileId,
        List<Long> evidenceFileIds,
        @NotBlank(message = "신청 사유는 필수입니다.")
        String reason,
        String plannedCenterName
) {}
