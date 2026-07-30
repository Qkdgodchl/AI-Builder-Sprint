package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.common.PageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService service;

    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<OpportunityResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.search(type, category, region, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OpportunityResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(service.publicDetail(id));
    }
}
