package com.pixelcare.domain.management.service;

import com.pixelcare.domain.management.dto.*;
import com.pixelcare.domain.management.repository.ManagementRepository;
import com.pixelcare.domain.user.repository.UserAccountRepository;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagementService {

    private final ManagementRepository repository;
    private final UserAccountRepository userRepository;

    public ManagementService(ManagementRepository repository, UserAccountRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ManagerApplicationResponse applyManager(Long userId, ManagerApplicationRequest request) {
        if (repository.hasPendingManagerApplication(userId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PENDING_APPLICATION_EXISTS",
                    "이미 검토 중인 관리자 신청이 있습니다."
            );
        }
        String publicId = repository.createManagerApplication(userId, request);
        return getManagerApplication(publicId, userId, false);
    }

    public List<ManagerApplicationResponse> myManagerApplications(Long userId) {
        return repository.findManagerApplicationsByUser(userId);
    }

    public ManagerApplicationResponse getManagerApplication(
            String publicId,
            Long requesterId,
            boolean operator
    ) {
        ManagerApplicationResponse application = repository.findManagerApplication(publicId)
                .orElseThrow(() -> notFound("관리자 신청을 찾을 수 없습니다."));
        if (!operator && !application.applicantUserId().equals(requesterId)) {
            throw forbidden();
        }
        return application;
    }

    @Transactional
    public ManagerApplicationResponse cancelManagerApplication(String publicId, Long userId) {
        ManagerApplicationResponse application = getManagerApplication(publicId, userId, false);
        requirePending(application);
        repository.cancelManagerApplication(publicId, userId);
        return getManagerApplication(publicId, userId, false);
    }

    public List<ManagerApplicationResponse> managerApplicationsForOperator(String status) {
        return repository.findManagerApplications(status);
    }

    @Transactional
    public ManagerApplicationResponse decideManagerApplication(
            String publicId,
            Long operatorId,
            String decision,
            String reason
    ) {
        ManagerApplicationResponse application = repository.findManagerApplication(publicId)
                .orElseThrow(() -> notFound("관리자 신청을 찾을 수 없습니다."));
        requirePending(application);
        repository.decideManagerApplication(publicId, decision, operatorId, reason);
        if ("APPROVED".equals(decision)) {
            userRepository.grantRole(application.applicantUserId(), "CENTER_MANAGER");
            repository.ensureOrganizationForApprovedApplication(application);
        }
        return repository.findManagerApplication(publicId).orElseThrow();
    }

    public List<OrganizationResponse> managedOrganizations(Long userId) {
        return repository.findManagedOrganizations(userId);
    }

    public OrganizationResponse publicOrganization(Long organizationId) {
        return repository.findOrganization(organizationId, true)
                .orElseThrow(() -> notFound("센터를 찾을 수 없습니다."));
    }

    @Transactional
    public OrganizationResponse updateOrganization(
            Long userId,
            Long organizationId,
            OrganizationUpdateRequest request
    ) {
        requireManager(userId, organizationId);
        repository.updateOrganization(organizationId, request);
        return repository.findOrganization(organizationId, false)
                .orElseThrow(() -> notFound("센터를 찾을 수 없습니다."));
    }

    public CenterDashboardResponse dashboard(Long userId, Long organizationId) {
        requireManager(userId, organizationId);
        return repository.dashboard(organizationId);
    }

    public void requireManager(Long userId, Long organizationId) {
        if (!repository.managesOrganization(userId, organizationId)) {
            throw forbidden();
        }
    }

    private void requirePending(ManagerApplicationResponse application) {
        if (!"PENDING".equals(application.status())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVALID_APPLICATION_STATE",
                    "대기 중인 신청만 처리할 수 있습니다."
            );
        }
    }

    private ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.");
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
