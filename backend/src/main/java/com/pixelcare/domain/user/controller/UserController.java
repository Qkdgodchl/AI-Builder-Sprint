package com.pixelcare.domain.user.controller;

import com.pixelcare.domain.user.dto.ProfileUpdateRequest;
import com.pixelcare.domain.user.dto.UserProfileResponse;
import com.pixelcare.domain.user.service.AuthService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthGuard authGuard;
    private final AuthService authService;

    public UserController(AuthGuard authGuard, AuthService authService) {
        this.authGuard = authGuard;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(HttpServletRequest request) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(authService.profile(user.id()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        CurrentUser user = authGuard.requireUser(httpRequest);
        return ApiResponse.success(authService.updateProfile(user.id(), request));
    }
}
