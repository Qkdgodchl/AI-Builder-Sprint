package com.pixelcare.domain.file.service;

import com.pixelcare.domain.file.dto.StoredFileResponse;
import com.pixelcare.domain.file.repository.StoredFileRepository;
import com.pixelcare.global.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class StoredFileService {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private final StoredFileRepository repository;
    private final Path storagePath;

    public StoredFileService(
            StoredFileRepository repository,
            @Value("${app.storage.path:storage}") String storagePath
    ) {
        this.repository = repository;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional
    public StoredFileResponse store(Long userId, String purpose, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED", "업로드할 파일이 필요합니다.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE",
                    "파일은 10MB 이하만 업로드할 수 있습니다."
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_FILE_TYPE",
                    "PDF, PNG, JPG 파일만 업로드할 수 있습니다."
            );
        }
        String originalName = sanitize(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + extension(originalName);
        Path target = storagePath.resolve(storageKey).normalize();
        if (!target.startsWith(storagePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_NAME", "잘못된 파일명입니다.");
        }
        try {
            Files.createDirectories(storagePath);
            byte[] bytes = file.getBytes();
            String checksum = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
            Files.copy(
                    new java.io.ByteArrayInputStream(bytes),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return repository.create(
                    userId,
                    storageKey,
                    originalName,
                    contentType,
                    file.getSize(),
                    checksum,
                    purpose == null || purpose.isBlank() ? "EVIDENCE" : purpose.toUpperCase()
            );
        } catch (Exception e) {
            try {
                Files.deleteIfExists(target);
            } catch (Exception ignored) {}
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_STORAGE_FAILED",
                    "파일 저장에 실패했습니다."
            );
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "upload";
        }
        return Path.of(name).getFileName().toString();
    }

    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index).toLowerCase();
    }
}
