package com.pixelcare.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "pixelCareCurrentUser";
    private final TokenService tokenService;

    public AuthTokenFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token != null) {
            try {
                request.setAttribute(CURRENT_USER_ATTRIBUTE, tokenService.authenticate(token));
            } catch (RuntimeException ignored) {
                // 보호 API에서 AuthGuard가 일관된 401 응답을 반환한다.
            }
        }
        filterChain.doFilter(request, response);
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
