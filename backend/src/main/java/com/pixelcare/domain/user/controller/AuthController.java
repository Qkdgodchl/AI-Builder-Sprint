package com.pixelcare.domain.user.controller;

import com.pixelcare.domain.user.dto.*;
import com.pixelcare.domain.user.service.AuthService;
import com.pixelcare.global.auth.AuthTokenFilter;
import com.pixelcare.global.auth.TokenService;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request), "회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "로그인했습니다.");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request), "토큰을 재발급했습니다.");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request
    ) {
        tokenService.logout(
                AuthTokenFilter.extractBearerToken(httpRequest),
                request == null ? null : request.refreshToken()
        );
        return ResponseEntity.noContent().build();
    }
}
