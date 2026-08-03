package com.pixelcare.domain.connect.controller;

import com.pixelcare.domain.connect.dto.ConnectDtos;
import com.pixelcare.domain.connect.service.ConnectService;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connect/requests")
public class ConnectController {

    private final ConnectService connectService;
    private final ManagementService managementService;
    private final com.pixelcare.domain.opportunity.service.OpportunityService opportunityService;
    private final AuthGuard authGuard;

    public ConnectController(ConnectService connectService,
                             ManagementService managementService,
                             com.pixelcare.domain.opportunity.service.OpportunityService opportunityService,
                             AuthGuard authGuard) {
        this.connectService = connectService;
        this.managementService = managementService;
        this.opportunityService = opportunityService;
        this.authGuard = authGuard;
    }

    /** 보는 사람이 맡을 수 있는 요청인지 판단하려면 그가 맡은 센터를 알아야 한다. */
    private java.util.Set<Long> managedOrganizationIds(CurrentUser viewer) {
        if (viewer == null || !viewer.hasRole("CENTER_MANAGER")) return java.util.Set.of();
        return managementService.managedOrganizations(viewer.id()).stream()
                .map(organization -> organization.id())
                .collect(java.util.stream.Collectors.toSet());
    }

    /** 목록은 로그인 없이도 볼 수 있다. 어떤 수요가 있는지는 누구나 보는 게 낫다. */
    @GetMapping
    public ApiResponse<List<ConnectDtos.Response>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String category
    ) {
        CurrentUser viewer = authGuard.resolveUser(request);
        return ApiResponse.success(
                connectService.list(category, viewer, managedOrganizationIds(viewer)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConnectDtos.Response> create(
            HttpServletRequest request,
            @Valid @RequestBody ConnectDtos.CreateRequest body
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(connectService.create(body, user), "센터에 요청을 전달했습니다.");
    }

    @PostMapping("/{publicId}/support")
    public ApiResponse<ConnectDtos.Response> support(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(connectService.toggleSupport(publicId, user));
    }

    /** 센터 관리자가 요청을 맡거나, 연 프로그램을 연결한다. */
    @PostMapping("/{publicId}/handle")
    public ApiResponse<ConnectDtos.Response> handle(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody(required = false) ConnectDtos.HandleRequest body
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        var organizations = managementService.managedOrganizations(user.id());
        if (organizations.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NO_MANAGED_ORGANIZATION",
                    "담당 센터가 있어야 요청을 맡을 수 있습니다.");
        }
        var organization = organizations.get(0);
        if (body != null && body.opportunityId() != null) {
            // 존재하지 않거나 내 센터 소유가 아니거나 공개 전인 프로그램을 연결하면
            // 요청자가 받는 "개설된 프로그램 보기" 링크가 끊긴다.
            var opportunity = opportunityService.managerDetail(user.id(), body.opportunityId());
            if (!"PUBLISHED".equals(opportunity.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "OPPORTUNITY_NOT_PUBLISHED",
                        "공개(PUBLISHED)된 프로그램만 연결할 수 있습니다.");
            }
            // 연결한 프로그램을 소유한 센터 이름으로 기록해야 안내가 정확하다.
            if (!opportunity.organizationId().equals(organization.id())) {
                var owning = organizations.stream()
                        .filter(candidate -> candidate.id().equals(opportunity.organizationId()))
                        .findFirst();
                if (owning.isPresent()) {
                    organization = owning.get();
                }
            }
        }
        return ApiResponse.success(
                connectService.handle(publicId, body, organization.id(), organization.name(), user),
                "요청을 맡았습니다.");
    }
}
