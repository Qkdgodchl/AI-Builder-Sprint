package com.pixelcare.domain.file.service;

import com.pixelcare.domain.file.repository.StoredFileRepository;
import com.pixelcare.global.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** 업로드된 사진을 화면에 그대로 내려주기 위한 읽기 전용 서비스. */
@Service
public class StoredFileReader {

    /** 목록·상세에 바로 박히는 사진이라 로그인 없이도 볼 수 있어야 한다. */
    private static final Set<String> PUBLIC_PURPOSES = Set.of("COMMUNITY_PHOTO", "ACTIVITY_PHOTO");
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg");

    private final StoredFileRepository repository;
    private final Path storagePath;

    public StoredFileReader(
            StoredFileRepository repository,
            @Value("${app.storage.path:storage}") String storagePath
    ) {
        this.repository = repository;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public record Content(PathResource resource, String contentType, String filename) {}

    public Content readPublicImage(Long fileId) {
        StoredFileRepository.StoredFileLocation file = repository.findLocation(fileId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."));

        if (!PUBLIC_PURPOSES.contains(file.purpose()) || !IMAGE_TYPES.contains(file.contentType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FILE_NOT_PUBLIC", "공개된 사진이 아닙니다.");
        }

        Path target = storagePath.resolve(file.storageKey()).normalize();
        if (!target.startsWith(storagePath) || !Files.isRegularFile(target)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다.");
        }
        return new Content(new PathResource(target), file.contentType(), file.originalName());
    }
}
