package com.pixelcare.domain.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String name,
        String phone,
        LocalDate birthDate,
        String region,
        Set<String> roles,
        List<String> interests,
        String accountStatus,
        Double temperature,
        LocalDateTime createdAt
) {}
