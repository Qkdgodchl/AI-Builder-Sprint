package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.dto.ClmDocumentFileResponse;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.entity.ClmDocumentFile;
import com.pixelcare.domain.clm.repository.ClmDocumentFileRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ClmDocumentArchiveService {

    public record DownloadedFile(PathResource resource, String filename, String contentType) {}

    private static final String SIGNED_DOCUMENT = "SIGNED_DOCUMENT";
    private static final String AUDIT_TRAIL = "AUDIT_TRAIL";

    private final ClmDocumentFileRepository fileRepository;
    private final ClmDocumentRepository documentRepository;
    private final ModusignApiClient modusignApiClient;
    private final Path archivePath;

    public ClmDocumentArchiveService(
            ClmDocumentFileRepository fileRepository,
            ClmDocumentRepository documentRepository,
            ModusignApiClient modusignApiClient,
            @Value("${app.storage.path:storage}") String storagePath
    ) {
        this.fileRepository = fileRepository;
        this.documentRepository = documentRepository;
        this.modusignApiClient = modusignApiClient;
        this.archivePath = Path.of(storagePath).toAbsolutePath().normalize().resolve("clm");
    }

    @Transactional
    public void archiveCompletedFiles(ClmDocument document) {
        if (fileRepository.existsByClmDocumentIdAndFileTypeAndIsDeletedFalse(document.getId(), SIGNED_DOCUMENT)) {
            return;
        }
        ModusignApiClient.CompletedDocumentFiles files =
                modusignApiClient.downloadCompletedDocumentFiles(document.getModusignDocumentId());
        store(document, SIGNED_DOCUMENT, "signed-document.pdf", files.signedDocument());
        if (files.auditTrail() != null && files.auditTrail().length > 0) {
            store(document, AUDIT_TRAIL, "audit-trail.pdf", files.auditTrail());
        }
    }

    @Transactional(readOnly = true)
    public List<ClmDocumentFileResponse> list(Long documentId, CurrentUser user) {
        requireDocumentAccess(documentId, user);
        return fileRepository.findByClmDocumentIdAndIsDeletedFalseOrderByIdAsc(documentId)
                .stream()
                .map(ClmDocumentFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(Long documentId, Long fileId, CurrentUser user) {
        requireDocumentAccess(documentId, user);
        ClmDocumentFile file = fileRepository.findByIdAndIsDeletedFalse(fileId)
                .filter(found -> found.getClmDocumentId().equals(documentId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_FILE_NOT_FOUND", "보관된 전자서명 파일을 찾을 수 없습니다."));
        Path target = archivePath.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(archivePath) || !Files.isRegularFile(target)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CLM_FILE_NOT_FOUND", "보관된 전자서명 파일을 찾을 수 없습니다.");
        }
        return new DownloadedFile(new PathResource(target), file.getOriginalName(), file.getContentType());
    }

    private void store(ClmDocument document, String type, String suffix, byte[] bytes) {
        String key = document.getId() + "-" + UUID.randomUUID() + ".pdf";
        Path target = archivePath.resolve(key).normalize();
        try {
            Files.createDirectories(archivePath);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String filename = "pixelcare-" + document.getId() + "-" + suffix;
            fileRepository.save(new ClmDocumentFile(
                    document.getId(), type, key, filename, "application/pdf", bytes.length, hash
            ));
        } catch (Exception e) {
            try {
                Files.deleteIfExists(target);
            } catch (Exception ignored) {}
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CLM_ARCHIVE_FAILED", "완료 전자서명 파일 보관에 실패했습니다.");
        }
    }

    private ClmDocument requireDocumentAccess(Long documentId, CurrentUser user) {
        ClmDocument document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_DOCUMENT_NOT_FOUND", "전자서명 서류를 찾을 수 없습니다."));
        // 센터 관리자는 향후 application/organization FK로 소속 센터가 확인될 때만 허용한다.
        // 단순 MANAGER 역할만으로 전체 서류를 열면 타 센터 개인정보가 노출된다.
        boolean staff = user.hasRole("OPERATOR") || user.hasRole("ROLE_OPERATOR") || user.hasRole("CENTER_MANAGER") || user.hasRole("MANAGER");
        if (!staff && !user.id().equals(document.getApplicantUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CLM_DOCUMENT_FORBIDDEN", "해당 전자서명 서류를 볼 권한이 없습니다.");
        }
        return document;
    }
}
