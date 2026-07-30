package com.pixelcare.domain.management.controller;

import com.pixelcare.domain.application.controller.OperatorDocumentController;
import com.pixelcare.domain.application.service.ApplicationService;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.error.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperatorManagementControllerTest {

    MockMvc managementMvc;
    MockMvc documentMvc;
    AuthGuard authGuard;
    ManagementService managementService;
    ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        authGuard = mock(AuthGuard.class);
        managementService = mock(ManagementService.class);
        applicationService = mock(ApplicationService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        managementMvc = MockMvcBuilders
                .standaloneSetup(
                        new OperatorManagerApplicationController(authGuard, managementService),
                        new OperatorOrganizationApplicationController(authGuard, managementService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        documentMvc = MockMvcBuilders
                .standaloneSetup(new OperatorDocumentController(authGuard, applicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void operatorCanListApprovalsAndDocuments() throws Exception {
        CurrentUser operator = new CurrentUser(
                1L, "operator@pixelcare.local", "운영진", Set.of("OPERATOR")
        );
        when(authGuard.requireRole(any(HttpServletRequest.class), eq("OPERATOR")))
                .thenReturn(operator);
        when(managementService.managerApplicationsForOperator(null)).thenReturn(List.of());
        when(managementService.organizationApplicationsForOperator(null)).thenReturn(List.of());
        when(applicationService.operatorApplications()).thenReturn(List.of());

        managementMvc.perform(get("/api/v1/operator/manager-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        managementMvc.perform(get("/api/v1/operator/organization-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        documentMvc.perform(get("/api/v1/operator/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void nonOperatorIsRejected() throws Exception {
        when(authGuard.requireRole(any(HttpServletRequest.class), eq("OPERATOR")))
                .thenThrow(new ApiException(
                        HttpStatus.FORBIDDEN, "ROLE_REQUIRED", "필요한 권한이 없습니다."
                ));

        managementMvc.perform(get("/api/v1/operator/manager-applications"))
                .andExpect(status().isForbidden());
        documentMvc.perform(get("/api/v1/operator/documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalRequiresReason() throws Exception {
        managementMvc.perform(post("/api/v1/operator/manager-applications/test-id/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
