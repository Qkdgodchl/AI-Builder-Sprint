package com.pixelcare.domain.application.service;

import com.pixelcare.domain.application.dto.*;
import com.pixelcare.domain.application.repository.ApplicationRepository;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;
    private final OpportunityService opportunityService;
    private final ManagementService managementService;
    private final com.pixelcare.domain.user.service.WarmthService warmthService;

    public ApplicationService(
            ApplicationRepository repository,
            OpportunityService opportunityService,
            ManagementService managementService,
            com.pixelcare.domain.user.service.WarmthService warmthService
    ) {
        this.repository = repository;
        this.opportunityService = opportunityService;
        this.managementService = managementService;
        this.warmthService = warmthService;
    }

    @Transactional
    public ApplicationResponse apply(
            Long userId,
            Long opportunityId,
            ApplicationCreateRequest request
    ) {
        OpportunityResponse opportunity = opportunityService.publicDetail(opportunityId);
        if (opportunity.recruitmentEndDateTime() != null
                && opportunity.recruitmentEndDateTime().isBefore(java.time.LocalDateTime.now())) {
            throw conflict("모집이 종료된 프로그램입니다.");
        }
        List<ApplicationResponse> existingList = repository.findByUser(userId);
        java.util.Optional<ApplicationResponse> match = existingList.stream()
                .filter(app -> app.opportunityId().equals(opportunityId))
                .findFirst();
        if (match.isPresent()) {
            ApplicationResponse existing = match.get();
            if (existing.commitment() != null && existing.commitment().publicId() != null) {
                return existing;
            }
            repository.ensureCommitmentExists(userId, existing.publicId(), opportunity, request);
            return repository.findByPublicId(existing.publicId()).orElse(existing);
        }
        if (opportunity.recruitmentCapacity() != null
                && repository.activeApplicationCount(opportunityId) >= opportunity.recruitmentCapacity()) {
            throw conflict("모집 정원이 마감되었습니다.");
        }
        if (request.participationDate() != null
                && request.participationDate().isBefore(LocalDate.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PARTICIPATION_DATE",
                    "참여일은 오늘보다 빠를 수 없습니다."
            );
        }
        String publicId = repository.create(userId, opportunity, request);
        return repository.findByPublicId(publicId).orElseThrow();
    }

    public List<ApplicationResponse> myApplications(Long userId) {
        return repository.findByUser(userId);
    }

    public List<ApplicationResponse> operatorApplications() {
        return repository.findAllForOperator();
    }

    public ApplicationResponse operatorDetail(String publicId) {
        return repository.findByPublicId(publicId).orElseThrow(() -> notFound());
    }

    public ApplicationResponse detail(Long userId, boolean manager, String publicId) {
        ApplicationResponse application = repository.findByPublicId(publicId)
                .orElseThrow(() -> notFound());
        boolean owner = application.applicantUserId().equals(userId);
        boolean centerManager = manager && repository.managerCanAccess(userId, publicId);
        if (!owner && !centerManager) {
            throw forbidden();
        }
        return application;
    }

    @Transactional
    public ApplicationResponse cancel(Long userId, String publicId) {
        ApplicationResponse application = detail(userId, false, publicId);
        if (!Set.of("APPLIED", "IN_REVIEW", "REVISION_REQUESTED").contains(application.status())) {
            throw conflict("현재 상태에서는 신청을 취소할 수 없습니다.");
        }
        repository.cancel(publicId, userId);
        return repository.findByPublicId(publicId).orElseThrow();
    }

    public List<ApplicationResponse> managerApplications(Long managerId, Long opportunityId) {
        OpportunityResponse opportunity = opportunityService.managerDetail(managerId, opportunityId);
        managementService.requireManager(managerId, opportunity.organizationId());
        return repository.findByOpportunity(opportunityId);
    }

    @Transactional
    public ApplicationResponse decide(
            Long managerId,
            String publicId,
            String decision,
            String reason
    ) {
        ApplicationResponse application = detail(managerId, true, publicId);
        if (!Set.of("APPLIED", "IN_REVIEW", "REVISION_REQUESTED").contains(application.status())) {
            throw conflict("이미 처리된 신청입니다.");
        }
        if (("REJECTED".equals(decision) || "REVISION_REQUESTED".equals(decision))
                && (reason == null || reason.isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REASON_REQUIRED",
                    "처리 사유를 입력해주세요."
            );
        }
        repository.decide(publicId, managerId, decision, reason);
        return repository.findByPublicId(publicId).orElseThrow();
    }

    public ApplicationResponse.CommitmentSummary commitment(Long userId, String commitmentPublicId) {
        if (!repository.ownsCommitment(userId, commitmentPublicId)) {
            throw forbidden();
        }
        return repository.findCommitment(commitmentPublicId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "COMMITMENT_NOT_FOUND",
                        "약정서를 찾을 수 없습니다."
                ));
    }

    @Transactional
    public ApplicationResponse.CommitmentSummary updateCommitment(
            Long userId,
            String commitmentPublicId,
            CommitmentUpdateRequest request
    ) {
        ApplicationResponse.CommitmentSummary current = commitment(userId, commitmentPublicId);
        if (!Set.of("DRAFT", "REVISION_REQUESTED").contains(current.status())) {
            throw conflict("현재 상태에서는 약정서를 수정할 수 없습니다.");
        }
        if (request.effectiveFrom() != null
                && request.effectiveTo() != null
                && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_COMMITMENT_DATE",
                    "약정 종료일은 시작일보다 빠를 수 없습니다."
            );
        }
        repository.updateCommitment(commitmentPublicId, userId, request);
        return repository.findCommitment(commitmentPublicId).orElseThrow();
    }

    @Transactional
    public ApplicationResponse.CommitmentSummary submitCommitment(
            Long userId,
            String commitmentPublicId
    ) {
        ApplicationResponse.CommitmentSummary current = commitment(userId, commitmentPublicId);
        if ("SUBMITTED".equals(current.status()) || "SIGNING".equals(current.status()) || "ACTIVE".equals(current.status())) {
            return current;
        }
        if (!Set.of("DRAFT", "REVISION_REQUESTED").contains(current.status())) {
            throw conflict("이미 제출했거나 제출할 수 없는 약정서입니다.");
        }
        repository.submitCommitment(commitmentPublicId);
        warmthService.awardQuietly(userId,
                com.pixelcare.domain.user.service.WarmthService.Reason.APPLICATION_SUBMITTED);
        return repository.findCommitment(commitmentPublicId).orElseThrow();
    }

    @Transactional
    public ApplicationResponse.CommitmentSummary renewCommitment(
            Long userId, String commitmentPublicId, CommitmentRenewalRequest request
    ) {
        ApplicationResponse.CommitmentSummary current = commitment(userId, commitmentPublicId);
        if (!"ACTIVE".equals(current.status())) {
            throw conflict("활성 상태의 정기 약정만 갱신할 수 있습니다.");
        }
        if (!Set.of("MONTHLY", "ANNUAL").contains(current.pledgeFrequency())) {
            throw conflict("월간 또는 연간 약정만 갱신할 수 있습니다.");
        }
        validateRequestedTerms(request);
        repository.renewCommitment(commitmentPublicId, userId, request, hasTermChange(current, request));
        warmthService.awardQuietly(userId,
                com.pixelcare.domain.user.service.WarmthService.Reason.COMMITMENT_RENEWED);
        return repository.findCommitment(commitmentPublicId).orElseThrow();
    }

    private void validateRequestedTerms(CommitmentRenewalRequest request) {
        if (request == null) return;
        if (request.pledgeAmount() != null
                && request.pledgeAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw conflict("변경할 약정 금액은 0보다 커야 합니다.");
        }
        if (request.pledgeFrequency() != null
                && !Set.of("MONTHLY", "ANNUAL").contains(request.pledgeFrequency())) {
            throw conflict("정기 약정 주기는 매월 또는 매년만 선택할 수 있습니다.");
        }
    }

    /**
     * 금액이나 주기가 실제로 달라졌는지 본다.
     * 같은 값을 그대로 보내온 경우는 조건 변경으로 치지 않는다.
     */
    private boolean hasTermChange(
            ApplicationResponse.CommitmentSummary current, CommitmentRenewalRequest request
    ) {
        if (request == null) return false;
        boolean amountChanged = request.pledgeAmount() != null
                && (current.pledgeAmount() == null
                    || current.pledgeAmount().compareTo(request.pledgeAmount()) != 0);
        boolean frequencyChanged = request.pledgeFrequency() != null
                && !request.pledgeFrequency().equals(current.pledgeFrequency());
        return amountChanged || frequencyChanged;
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "APPLICATION_STATE_CONFLICT", message);
    }

    private ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "신청에 접근할 권한이 없습니다.");
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "신청을 찾을 수 없습니다.");
    }
}
