package com.pixelcare.domain.clm.controller;

import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.service.ClmDocumentService;
import com.pixelcare.domain.clm.service.ClmDocumentArchiveService;
import com.pixelcare.domain.clm.service.ClmDocumentVerificationService;
import com.pixelcare.domain.clm.dto.ClmDocumentFileResponse;
import com.pixelcare.domain.clm.dto.ClmVerificationResponse;
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
    private final ClmDocumentVerificationService verificationService;

    public ClmDocumentController(ClmDocumentService clmDocumentService, AuthGuard authGuard,
                                 ClmDocumentArchiveService archiveService,
                                 ClmDocumentVerificationService verificationService) {
        this.clmDocumentService = clmDocumentService;
        this.authGuard = authGuard;
        this.archiveService = archiveService;
        this.verificationService = verificationService;
    }

    /** 보관된 약정서를 Upstage 문서 AI로 되읽어 약정 원본과 맞춰 본다. */
    @PostMapping("/{id}/verification")
    public ApiResponse<ClmVerificationResponse> verify(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.requireUser(httpRequest);
        return ApiResponse.success(verificationService.verify(id, currentUser), "약정서 검증을 마쳤습니다.");
    }

    /** 마지막 검증 결과. 아직 검증한 적이 없으면 data가 비어 있다. */
    @GetMapping("/{id}/verification")
    public ApiResponse<ClmVerificationResponse> latestVerification(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.requireUser(httpRequest);
        return ApiResponse.success(verificationService.findLatest(id, currentUser).orElse(null));
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

    /**
     * LLM 대화 기반 약정서 자동 생성 + 모두싸인 전자서명 요청
     * 1. AI 상담 세션(consultationId)에서 대화 내역 + PledgeIntent 조회
     * 2. Upstage Solar 추출 결과로 iText PDF 약정서 생성
     * 3. 모두싸인에 PDF 업로드 → 서명 요청
     */
    @PostMapping("/request-sign-from-conversation")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClmDocumentResponseDto> requestSignFromConversation(
            @RequestBody ConversationSignRequest request,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.requireUser(httpRequest);
        ClmDocumentResponseDto response = clmDocumentService.requestSignFromConversation(
                request.consultationId(),
                request.commitmentPublicId(),
                request.applicantPhone(),
                currentUser
        );
        return ApiResponse.success(response, "LLM 대화 기반 약정서 PDF가 생성되어 모두싸인 전자서명 요청이 완료되었습니다.");
    }

    public record ConversationSignRequest(
            Long consultationId,
            String commitmentPublicId,
            String applicantPhone
    ) {}
}
