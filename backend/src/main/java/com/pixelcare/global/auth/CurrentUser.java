package com.pixelcare.global.auth;

import java.util.Set;

public record CurrentUser(
        Long id,
        String email,
        String nickname,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
