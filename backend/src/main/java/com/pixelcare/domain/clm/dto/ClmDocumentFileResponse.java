package com.pixelcare.domain.clm.dto;

import com.pixelcare.domain.clm.entity.ClmDocumentFile;

import java.time.LocalDateTime;

public record ClmDocumentFileResponse(
        Long id,
        String fileType,
        String originalName,
        String contentType,
        Long sizeBytes,
        String sha256,
        LocalDateTime createdAt,
        String downloadUrl
) {
    public static ClmDocumentFileResponse from(ClmDocumentFile file) {
        return new ClmDocumentFileResponse(
                file.getId(),
                file.getFileType(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getSha256(),
                file.getCreatedAt(),
                "/api/v1/clm/documents/" + file.getClmDocumentId() + "/files/" + file.getId() + "/download"
        );
    }
}
