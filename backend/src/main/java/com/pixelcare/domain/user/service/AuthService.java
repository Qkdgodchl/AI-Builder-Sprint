package com.pixelcare.domain.user.service;

import com.pixelcare.domain.user.dto.*;
import com.pixelcare.domain.user.repository.UserAccountRepository;
import com.pixelcare.global.auth.AuthRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.auth.TokenService;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final AuthRepository authRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserAccountRepository userRepository,
            AuthRepository authRepository,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }
        Long userId = userRepository.create(request, passwordEncoder.encode(request.password()));
        CurrentUser user = authRepository.findCurrentUserById(userId)
                .orElseThrow(() -> new IllegalStateException("생성된 사용자를 찾을 수 없습니다."));
        TokenService.TokenPair tokens = tokenService.issue(userId);
        return toResponse(user, tokens);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccountRepository.UserCredential credential = userRepository
                .findCredentialByEmail(request.email())
                .orElseThrow(() -> invalidCredentials());
        if (!"ACTIVE".equals(credential.accountStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "사용할 수 없는 계정입니다.");
        }
        if (credential.passwordHash() == null
                || !passwordEncoder.matches(request.password(), credential.passwordHash())) {
            throw invalidCredentials();
        }
        userRepository.updateLastLogin(credential.id());
        CurrentUser user = authRepository.findCurrentUserById(credential.id())
                .orElseThrow(() -> invalidCredentials());
        return toResponse(user, tokenService.issue(user.id()));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        TokenService.TokenPair tokens = tokenService.refresh(request.refreshToken());
        CurrentUser user = tokenService.authenticate(tokens.accessToken());
        return toResponse(user, tokens);
    }

    public UserProfileResponse profile(Long userId) {
        return userRepository.findProfile(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."
                ));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        userRepository.updateProfile(
                userId,
                request.nickname(),
                request.phone(),
                request.region(),
                request.interests()
        );
        return profile(userId);
    }

    private AuthResponse toResponse(CurrentUser user, TokenService.TokenPair tokens) {
        return new AuthResponse(
                user.id(),
                user.email(),
                user.nickname(),
                user.roles(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn()
        );
    }

    private ApiException invalidCredentials() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "이메일 또는 비밀번호가 올바르지 않습니다."
        );
    }
}
