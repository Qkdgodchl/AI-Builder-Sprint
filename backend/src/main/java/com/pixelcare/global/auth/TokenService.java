package com.pixelcare.global.auth;

import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthRepository authRepository;

    public TokenService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Transactional
    public TokenPair issue(Long userId) {
        String accessToken = randomToken();
        String refreshToken = randomToken();
        authRepository.saveAccessToken(userId, hash(accessToken), LocalDateTime.now().plusMinutes(30));
        authRepository.saveRefreshToken(userId, hash(refreshToken), LocalDateTime.now().plusDays(14));
        return new TokenPair(accessToken, refreshToken, 1800L);
    }

    @Transactional(readOnly = true)
    public CurrentUser authenticate(String rawToken) {
        return authRepository.findUserByAccessTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_ACCESS_TOKEN",
                        "로그인이 필요하거나 토큰이 만료되었습니다."
                ));
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        String tokenHash = hash(refreshToken);
        Long userId = authRepository.findUserIdByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_REFRESH_TOKEN",
                        "Refresh Token이 만료되었거나 폐기되었습니다."
                ));
        authRepository.revokeRefreshToken(tokenHash);
        return issue(userId);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            authRepository.revokeAccessToken(hash(accessToken));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            authRepository.revokeRefreshToken(hash(refreshToken));
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시에 실패했습니다.", e);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, Long expiresIn) {}
}
