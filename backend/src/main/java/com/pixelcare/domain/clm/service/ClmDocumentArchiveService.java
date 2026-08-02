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
    private final ClmDocumentAccessService accessService;
    private final ModusignApiClient modusignApiClient;
    private final Path archivePath;

    public ClmDocumentArchiveService(
            ClmDocumentFileRepository fileRepository,
            ClmDocumentAccessService accessService,
            ModusignApiClient modusignApiClient,
            @Value("${app.storage.path:storage}") String storagePath
    ) {
        this.fileRepository = fileRepository;
        this.accessService = accessService;
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

    @Transactional
    public void archiveDraftPdf(ClmDocument document, byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) return;
        if (!fileRepository.existsByClmDocumentIdAndFileTypeAndIsDeletedFalse(document.getId(), "PLEDGE_DRAFT_PDF")) {
            store(document, "PLEDGE_DRAFT_PDF", "pledge-contract-draft.pdf", pdfBytes);
        }
    }

    @Transactional(readOnly = true)
    public List<ClmDocumentFileResponse> list(Long documentId, CurrentUser user) {
        accessService.requireAccess(documentId, user);
        return fileRepository.findByClmDocumentIdAndIsDeletedFalseOrderByIdAsc(documentId)
                .stream()
                .map(ClmDocumentFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(Long documentId, Long fileId, CurrentUser user) {
        accessService.requireAccess(documentId, user);
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
}
