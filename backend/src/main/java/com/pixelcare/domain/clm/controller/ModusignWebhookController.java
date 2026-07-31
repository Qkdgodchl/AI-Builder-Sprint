package com.pixelcare.domain.clm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pixelcare.domain.clm.service.ClmDocumentService;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/webhooks/modusign")
public class ModusignWebhookController {

    private final ClmDocumentService clmDocumentService;
    private final String webhookSecret;

    public ModusignWebhookController(
            ClmDocumentService clmDocumentService,
            @Value("${modusign.webhook.secret:}") String webhookSecret
    ) {
        this.clmDocumentService = clmDocumentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public ApiResponse<Void> receive(
            @RequestHeader(value = "X-Pixelcare-Webhook-Secret", required = false) String suppliedSecret,
            @RequestBody JsonNode body
    ) {
        verifySecret(suppliedSecret);
        String eventType = body.path("event").path("type").asText();
        String documentId = body.path("document").path("id").asText();
        if (eventType.isBlank() || documentId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MODUSIGN_WEBHOOK", "웹훅 이벤트 또는 문서 ID가 없습니다.");
        }
        clmDocumentService.applyWebhookEvent(documentId, eventType);
        return ApiResponse.success(null, "웹훅을 처리했습니다.");
    }

    private void verifySecret(String suppliedSecret) {
        if (webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MODUSIGN_WEBHOOK_NOT_CONFIGURED", "웹훅 비밀값 설정이 필요합니다.");
        }
        byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = suppliedSecret == null ? new byte[0] : suppliedSecret.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_MODUSIGN_WEBHOOK_SECRET", "웹훅 인증에 실패했습니다.");
        }
    }
}
