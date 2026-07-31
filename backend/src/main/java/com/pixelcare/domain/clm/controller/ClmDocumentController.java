package com.pixelcare.domain.clm.controller;

import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.service.ClmDocumentService;
import com.pixelcare.domain.clm.service.ClmDocumentArchiveService;
import com.pixelcare.domain.clm.dto.ClmDocumentFileResponse;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clm/documents")
public class ClmDocumentController {

    private final ClmDocumentService clmDocumentService;
    private final AuthGuard authGuard;
    private final ClmDocumentArchiveService archiveService;

    public ClmDocumentController(ClmDocumentService clmDocumentService, AuthGuard authGuard,
                                 ClmDocumentArchiveService archiveService) {
        this.clmDocumentService = clmDocumentService;
        this.authGuard = authGuard;
        this.archiveService = archiveService;
    }

    @PostMapping("/request-sign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClmDocumentResponseDto> requestSign(
            @Valid @RequestBody ClmSignRequestDto request,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.requireUser(httpRequest);
        ClmDocumentResponseDto response = clmDocumentService.requestSign(request, currentUser);
        return ApiResponse.success(response, "모두싸인 전자서명 요청 문서가 성공적으로 생성되었습니다.");
    }

    @PostMapping("/{id}/secure-link")
    public ApiResponse<ClmDocumentResponseDto> refreshSecureLink(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ClmDocumentResponseDto response = clmDocumentService.refreshSecureLink(id, authGuard.requireUser(request));
        return ApiResponse.success(response, "모두싸인 보안 서명 링크가 발급되었습니다.");
    }

    @GetMapping("/{id}")
    public ApiResponse<ClmDocumentResponseDto> getDocumentDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        ClmDocumentResponseDto response = clmDocumentService.getDocumentDetail(id, authGuard.requireUser(request));
        return ApiResponse.success(response);
    }

    @GetMapping("/my")
    public ApiResponse<List<ClmDocumentResponseDto>> getMyDocuments(HttpServletRequest request) {
        List<ClmDocumentResponseDto> list = clmDocumentService.getMyDocuments(authGuard.requireUser(request));
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}/files")
    public ApiResponse<List<ClmDocumentFileResponse>> getDocumentFiles(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return ApiResponse.success(archiveService.list(id, authGuard.requireUser(request)));
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadDocumentFile(
            @PathVariable Long id,
            @PathVariable Long fileId,
            HttpServletRequest request
    ) {
        ClmDocumentArchiveService.DownloadedFile file =
                archiveService.download(id, fileId, authGuard.requireUser(request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
                .body(file.resource());
    }
}
