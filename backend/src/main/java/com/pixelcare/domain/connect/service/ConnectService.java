package com.pixelcare.domain.connect.service;

import com.pixelcare.domain.connect.dto.ConnectDtos;
import com.pixelcare.domain.connect.entity.ConnectRequest;
import com.pixelcare.domain.connect.entity.ConnectSupport;
import com.pixelcare.domain.connect.repository.ConnectRequestRepository;
import com.pixelcare.domain.connect.repository.ConnectSupportRepository;
import com.pixelcare.domain.user.service.WarmthService;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ConnectService {

    private static final Set<String> CATEGORIES = Set.of("VOLUNTEER", "DONATION");

    private final ConnectRequestRepository requestRepository;
    private final ConnectSupportRepository supportRepository;
    private final WarmthService warmthService;

    public ConnectService(ConnectRequestRepository requestRepository,
                          ConnectSupportRepository supportRepository,
                          WarmthService warmthService) {
        this.requestRepository = requestRepository;
        this.supportRepository = supportRepository;
        this.warmthService = warmthService;
    }

    public List<ConnectDtos.Response> list(String category, CurrentUser viewer,
                                           Set<Long> viewerOrganizationIds) {
        List<ConnectRequest> requests = category != null && CATEGORIES.contains(category)
                ? requestRepository.findByCategoryAndIsDeletedFalseOrderBySupportCountDescIdDesc(category)
                : requestRepository.findByIsDeletedFalseOrderBySupportCountDescIdDesc();

        // 로그인한 사람이 이미 공감한 요청을 한 번에 모아 두고 표시한다.
        Set<Long> supported = viewer == null ? Set.of()
                : supportRepository.findByUserId(viewer.id()).stream()
                        .map(ConnectSupport::getRequestId)
                        .collect(java.util.stream.Collectors.toSet());

        boolean viewerIsManager = viewer != null && viewer.hasRole("CENTER_MANAGER")
                && !viewerOrganizationIds.isEmpty();

        return requests.stream()
                .map(request -> ConnectDtos.Response.of(
                        request,
                        supported.contains(request.getId()),
                        viewer != null && viewer.id().equals(request.getRequesterUserId()),
                        viewerOrganizationIds,
                        viewerIsManager))
                .toList();
    }

    @Transactional
    public ConnectDtos.Response create(ConnectDtos.CreateRequest body, CurrentUser user) {
        String category = body.category() != null && CATEGORIES.contains(body.category())
                ? body.category() : "VOLUNTEER";
        String origin = "AI".equals(body.origin()) ? "AI" : "DIRECT";

        ConnectRequest saved = requestRepository.save(new ConnectRequest(
                user.id(), user.nickname(), body.title().trim(), body.content().trim(),
                category, body.region(), origin));
        warmthService.awardQuietly(user.id(), WarmthService.Reason.POST_WRITTEN);
        return ConnectDtos.Response.of(saved, false, true);
    }

    /** 나도 원한다는 표시를 켜고 끈다. 수요가 얼마나 되는지가 센터의 판단 근거다. */
    @Transactional
    public ConnectDtos.Response toggleSupport(String publicId, CurrentUser user) {
        ConnectRequest request = require(publicId);
        var existing = supportRepository.findByRequestIdAndUserId(request.getId(), user.id());
        boolean supported;
        if (existing.isPresent()) {
            supportRepository.delete(existing.get());
            request.changeSupportCount(-1);
            supported = false;
        } else {
            supportRepository.save(new ConnectSupport(request.getId(), user.id()));
            request.changeSupportCount(1);
            supported = true;
            warmthService.awardQuietly(user.id(), WarmthService.Reason.POST_LIKED);
        }
        requestRepository.save(request);
        return ConnectDtos.Response.of(request, supported,
                user.id().equals(request.getRequesterUserId()));
    }

    /** 센터가 요청을 맡는다. */
    @Transactional
    public ConnectDtos.Response handle(String publicId, ConnectDtos.HandleRequest body,
                                       Long organizationId, String organizationName, CurrentUser user) {
        ConnectRequest request = require(publicId);
        if (ConnectRequest.FULFILLED.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONNECT_ALREADY_FULFILLED",
                    "이미 프로그램으로 열린 요청입니다.");
        }
        // 먼저 맡은 센터가 있으면 다른 센터가 덮어쓰지 못하게 막는다.
        Long owner = request.getHandledOrganizationId();
        if (owner != null && !owner.equals(organizationId)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONNECT_ALREADY_HANDLED",
                    "%s에서 이미 맡은 요청입니다.".formatted(request.getHandledOrganizationName()));
        }

        if (body != null && body.opportunityId() != null) {
            request.markReviewing(organizationId, organizationName, body.message());
            request.markFulfilled(body.opportunityId(), body.message());
        } else {
            request.markReviewing(organizationId, organizationName,
                    body == null ? null : body.message());
        }

        try {
            requestRepository.saveAndFlush(request);
        } catch (org.springframework.dao.OptimisticLockingFailureException race) {
            // 같은 순간에 다른 센터가 먼저 저장한 경우다.
            throw new ApiException(HttpStatus.CONFLICT, "CONNECT_ALREADY_HANDLED",
                    "방금 다른 센터가 이 요청을 맡았습니다. 새로고침해 주세요.");
        }
        return ConnectDtos.Response.of(request, false,
                user.id().equals(request.getRequesterUserId()),
                Set.of(organizationId), true);
    }

    private ConnectRequest require(String publicId) {
        return requestRepository.findByPublicIdAndIsDeletedFalse(publicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "CONNECT_REQUEST_NOT_FOUND", "요청을 찾을 수 없습니다."));
    }
}
