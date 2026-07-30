package com.pixelcare.domain.clm.controller;

import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.service.ClmDocumentService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clm/documents")
public class ClmDocumentController {

    private final ClmDocumentService clmDocumentService;
    private final AuthGuard authGuard;

    public ClmDocumentController(ClmDocumentService clmDocumentService, AuthGuard authGuard) {
        this.clmDocumentService = clmDocumentService;
        this.authGuard = authGuard;
    }

    @PostMapping("/request-sign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClmDocumentResponseDto> requestSign(
            @Valid @RequestBody ClmSignRequestDto request,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.resolveUser(httpRequest);
        Long userId = currentUser != null ? currentUser.id() : null;

        ClmDocumentResponseDto response = clmDocumentService.requestSign(request, userId);
        return ApiResponse.success(response, "모두싸인 전자서명 요청 문서가 성공적으로 생성되었습니다.");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ClmDocumentResponseDto> completeSign(@PathVariable Long id) {
        ClmDocumentResponseDto response = clmDocumentService.completeSign(id);
        return ApiResponse.success(response, "전자서명이 성공적으로 완료되었습니다.");
    }

    @GetMapping("/{id}")
    public ApiResponse<ClmDocumentResponseDto> getDocumentDetail(@PathVariable Long id) {
        ClmDocumentResponseDto response = clmDocumentService.getDocumentDetail(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/my")
    public ApiResponse<List<ClmDocumentResponseDto>> getMyDocuments(@RequestParam String email) {
        List<ClmDocumentResponseDto> list = clmDocumentService.getMyDocuments(email);
        return ApiResponse.success(list);
    }
}
