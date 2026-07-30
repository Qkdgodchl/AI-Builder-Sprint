package com.pixelcare.domain.user.dto;

import java.util.Set;

public record AuthResponse(
        Long userId,
        String email,
        String nickname,
        Set<String> roles,
        String accessToken,
        String refreshToken,
        Long expiresIn
) {}
