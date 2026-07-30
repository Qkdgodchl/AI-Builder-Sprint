package com.pixelcare.domain.user.controller;

import com.pixelcare.domain.user.dto.AuthResponse;
import com.pixelcare.domain.user.service.AuthService;
import com.pixelcare.global.auth.TokenService;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    MockMvc mockMvc;
    AuthService authService;
    TokenService tokenService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        tokenService = mock(TokenService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, tokenService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void signupReturns201ForValidRequest() throws Exception {
        when(authService.signup(any())).thenReturn(new AuthResponse(
                1L,
                "member@example.com",
                "회원",
                Set.of("USER"),
                "access-token",
                "refresh-token",
                1800L
        ));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "password": "Password123!",
                                  "name": "홍길동",
                                  "privacyConsent": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void signupReturns400WhenConsentIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "password": "Password123!",
                                  "name": "홍길동",
                                  "privacyConsent": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginReturns401ForWrongCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "이메일 또는 비밀번호가 올바르지 않습니다."
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }
}
