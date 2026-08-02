package com.pixelcare.domain.clm.dto;

import com.pixelcare.domain.clm.service.ClmDocumentVerificationService.FieldCheck;

import java.time.LocalDateTime;
import java.util.List;

/** 체결본을 되읽어 약정 원본과 맞춰 본 결과. */
public record ClmVerificationResponse(
        Long id,
        Long documentId,
        String status,
        int checkedCount,
        int matchedCount,
        List<FieldCheck> checks,
        String parsedExcerpt,
        String sourceFileType,
        String provider,
        LocalDateTime verifiedAt
) {}
