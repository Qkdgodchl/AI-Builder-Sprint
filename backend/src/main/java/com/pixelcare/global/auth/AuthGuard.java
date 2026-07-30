package com.pixelcare.global.auth;

import com.pixelcare.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthGuard {

    public CurrentUser requireUser(HttpServletRequest request) {
        Object user = request.getAttribute(AuthTokenFilter.CURRENT_USER_ATTRIBUTE);
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
    }

    public CurrentUser requireRole(HttpServletRequest request, String role) {
        CurrentUser user = requireUser(request);
        if (!user.hasRole(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_REQUIRED", "필요한 권한이 없습니다.");
        }
        return user;
    }
}
