package com.pixelcare.domain.opportunity.service;

import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.domain.opportunity.dto.OpportunityRequest;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.domain.opportunity.repository.OpportunityRepository;
import com.pixelcare.global.common.PageResponse;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class OpportunityService {

    private static final Set<String> TYPES = Set.of(
            "VOLUNTEER",
            "DONATION",
            "HOMETOWN_DONATION",
            "CULTURAL_HERITAGE_DONATION",
            "LEGACY_DONATION"
    );

    private final OpportunityRepository repository;
    private final ManagementService managementService;

    public OpportunityService(
            OpportunityRepository repository,
            ManagementService managementService
    ) {
        this.repository = repository;
        this.managementService = managementService;
    }

    public PageResponse<OpportunityResponse> search(
            String type,
            String category,
            String region,
            String keyword,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest("페이지는 0 이상, size는 1~100이어야 합니다.");
        }
        OpportunityRepository.SearchResult result = repository.searchPublic(
                type,
                category,
                region,
                keyword,
                page,
                size
        );
        return PageResponse.of(result.items(), page, size, result.totalElements());
    }

    public OpportunityResponse publicDetail(Long id) {
        return repository.findPublicById(id)
                .orElseThrow(() -> notFound());
    }

    public OpportunityResponse managerDetail(Long userId, Long id) {
        OpportunityResponse opportunity = repository.findById(id)
                .orElseThrow(() -> notFound());
        managementService.requireManager(userId, opportunity.organizationId());
        return opportunity;
    }

    public List<OpportunityResponse> managerList(Long userId, Long organizationId) {
        managementService.requireManager(userId, organizationId);
        return repository.findByOrganization(organizationId);
    }

    @Transactional
    public OpportunityResponse create(
            Long userId,
            Long organizationId,
            OpportunityRequest request
    ) {
        managementService.requireManager(userId, organizationId);
        validate(request);
        Long id = repository.create(organizationId, userId, request);
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public OpportunityResponse update(Long userId, Long id, OpportunityRequest request) {
        OpportunityResponse current = managerDetail(userId, id);
        if (!Set.of("DRAFT", "PUBLISHED").contains(current.status())) {
            throw conflict("현재 상태에서는 모집글을 수정할 수 없습니다.");
        }
        validate(request);
        repository.update(id, request);
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public OpportunityResponse publish(Long userId, Long id) {
        OpportunityResponse current = managerDetail(userId, id);
        int updated = repository.transitionStatus(id, List.of("DRAFT"), "PUBLISHED");
        if (updated == 0) {
            throw conflict("초안 상태의 모집글만 공개할 수 있습니다.");
        }
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public OpportunityResponse close(Long userId, Long id) {
        managerDetail(userId, id);
        int updated = repository.transitionStatus(id, List.of("PUBLISHED"), "RECRUITMENT_CLOSED");
        if (updated == 0) {
            throw conflict("공개 중인 모집글만 마감할 수 있습니다.");
        }
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public OpportunityResponse cancel(Long userId, Long id) {
        managerDetail(userId, id);
        int updated = repository.transitionStatus(
                id,
                List.of("DRAFT", "PUBLISHED", "RECRUITMENT_CLOSED"),
                "CANCELLED"
        );
        if (updated == 0) {
            throw conflict("취소할 수 없는 모집글 상태입니다.");
        }
        return repository.findById(id).orElseThrow();
    }

    private void validate(OpportunityRequest request) {
        String type = request.type().toUpperCase();
        if (!TYPES.contains(type)) {
            throw badRequest("지원하지 않는 모집글 유형입니다.");
        }
        if (request.targetAmount() != null && request.targetAmount() < 0) {
            throw badRequest("목표 금액은 음수일 수 없습니다.");
        }
        if (request.recruitmentStartDateTime() != null
                && request.recruitmentEndDateTime() != null
                && request.recruitmentEndDateTime().isBefore(request.recruitmentStartDateTime())) {
            throw badRequest("모집 종료일은 모집 시작일보다 빠를 수 없습니다.");
        }
        if (request.activityStartDateTime() != null
                && request.activityEndDateTime() != null
                && request.activityEndDateTime().isBefore(request.activityStartDateTime())) {
            throw badRequest("활동 종료일은 활동 시작일보다 빠를 수 없습니다.");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPPORTUNITY", message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "OPPORTUNITY_STATE_CONFLICT", message);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "OPPORTUNITY_NOT_FOUND", "모집글을 찾을 수 없습니다.");
    }
}
