package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.common.PageResponse;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpportunityControllerTest {

    MockMvc mockMvc;
    OpportunityService opportunityService;

    @BeforeEach
    void setUp() {
        opportunityService = mock(OpportunityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OpportunityController(opportunityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsPaginationResponse() throws Exception {
        when(opportunityService.search(null, null, null, null, 0, 20))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void detailReturns404WhenOpportunityDoesNotExist() throws Exception {
        when(opportunityService.publicDetail(999L)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "프로그램을 찾을 수 없습니다."
        ));

        mockMvc.perform(get("/api/v1/opportunities/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
