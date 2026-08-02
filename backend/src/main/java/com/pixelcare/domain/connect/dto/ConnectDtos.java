package com.pixelcare.domain.connect.dto;

import com.pixelcare.domain.connect.entity.ConnectRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ConnectDtos {

    public record CreateRequest(
            @NotBlank(message = "어떤 활동을 원하시는지 한 줄로 적어주세요.")
            @Size(max = 120, message = "제목은 120자까지 쓸 수 있습니다.")
            String title,
            @NotBlank(message = "내용을 적어주세요.")
            String content,
            String category,
            String region,
            /** AI 추천이 비어 넘어온 요청이면 AI. */
            String origin
    ) {}

    public record HandleRequest(String message, Long opportunityId) {}

    public record Response(
            String publicId,
            String title,
            String content,
            String category,
            String region,
            String origin,
            String status,
            int supportCount,
            boolean supportedByMe,
            String requesterNickname,
            boolean mine,
            String handledOrganizationName,
            Long handledOpportunityId,
            String handledMessage,
            /** 이 요청을 맡은 센터가 보는 사람의 센터인지. */
            boolean handledByMe,
            /** 지금 이 사람이 맡거나 프로그램을 연결할 수 있는지. */
            boolean canHandle,
            LocalDateTime createdAt
    ) {
        public static Response of(ConnectRequest request, boolean supportedByMe, boolean mine) {
            return of(request, supportedByMe, mine, java.util.Set.of(), false);
        }

        public static Response of(ConnectRequest request, boolean supportedByMe, boolean mine,
                                  java.util.Set<Long> viewerOrganizationIds, boolean viewerIsManager) {
            boolean handledByMe = request.getHandledOrganizationId() != null
                    && viewerOrganizationIds.contains(request.getHandledOrganizationId());
            // 아직 아무도 안 맡았거나 내가 맡은 건만 손댈 수 있다.
            boolean canHandle = viewerIsManager
                    && !ConnectRequest.FULFILLED.equals(request.getStatus())
                    && (request.getHandledOrganizationId() == null || handledByMe);
            return new Response(
                    request.getPublicId(),
                    request.getTitle(),
                    request.getContent(),
                    request.getCategory(),
                    request.getRegion(),
                    request.getOrigin(),
                    request.getStatus(),
                    request.getSupportCount(),
                    supportedByMe,
                    request.getRequesterNickname(),
                    mine,
                    request.getHandledOrganizationName(),
                    request.getHandledOpportunityId(),
                    request.getHandledMessage(),
                    handledByMe,
                    canHandle,
                    request.getCreatedAt()
            );
        }
    }
}
