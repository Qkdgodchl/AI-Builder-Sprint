package com.pixelcare.domain.file.dto;

import java.time.LocalDateTime;

public record StoredFileResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        String purpose,
        LocalDateTime createdAt
) {}
