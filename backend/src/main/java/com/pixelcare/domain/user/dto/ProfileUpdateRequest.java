package com.pixelcare.domain.user.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfileUpdateRequest(
        @Size(min = 1, max = 100, message = "닉네임은 1자 이상 100자 이하여야 합니다.")
        String nickname,
        String phone,
        String region,
        List<String> interests
) {}
