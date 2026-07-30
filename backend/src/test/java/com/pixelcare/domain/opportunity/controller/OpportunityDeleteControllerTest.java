package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.error.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpportunityDeleteControllerTest {

    MockMvc mockMvc;
    AuthGuard authGuard;
    OpportunityService opportunityService;
    CurrentUser author;

    @BeforeEach
    void setUp() {
        authGuard = mock(AuthGuard.class);
        opportunityService = mock(OpportunityService.class);
        author = new CurrentUser(10L, "author@example.com", "작성자", Set.of("USER"));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OpportunityDeleteController(authGuard, opportunityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authorCanDeleteOpportunity() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);

        mockMvc.perform(delete("/api/v1/opportunities/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(opportunityService).deleteAsUser(author, 7L);
    }

    @Test
    void otherUserReceives403() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "OPPORTUNITY_DELETE_FORBIDDEN", "삭제 권한이 없습니다."))
                .when(opportunityService).deleteAsUser(author, 7L);

        mockMvc.perform(delete("/api/v1/opportunities/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unauthenticatedUserReceives401() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.")
        );

        mockMvc.perform(delete("/api/v1/opportunities/7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingOpportunityReceives404() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "OPPORTUNITY_NOT_FOUND", "모집글을 찾을 수 없습니다."))
                .when(opportunityService).deleteAsUser(author, 999L);

        mockMvc.perform(delete("/api/v1/opportunities/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
