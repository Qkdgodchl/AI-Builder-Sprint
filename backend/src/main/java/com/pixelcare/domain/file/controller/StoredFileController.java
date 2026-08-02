package com.pixelcare.domain.file.controller;

import com.pixelcare.domain.file.dto.StoredFileResponse;
import com.pixelcare.domain.file.service.StoredFileReader;
import com.pixelcare.domain.file.service.StoredFileService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class StoredFileController {

    private final AuthGuard authGuard;
    private final StoredFileService service;
    private final StoredFileReader reader;

    public StoredFileController(
            AuthGuard authGuard, StoredFileService service, StoredFileReader reader
    ) {
        this.authGuard = authGuard;
        this.service = service;
        this.reader = reader;
    }

    /** 커뮤니티·활동 기록 사진은 목록에 바로 박히므로 로그인 없이 조회한다. */
    @GetMapping("/{fileId}/image")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> image(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "false") boolean download
    ) {
        StoredFileReader.Content content = reader.readPublicImage(fileId);
        // 상대가 받은 사진을 그대로 저장할 수 있도록 내려받기 모드를 지원한다.
        String disposition = (download ? "attachment" : "inline")
                + "; filename*=UTF-8''"
                + java.net.URLEncoder.encode(content.filename(), java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, content.contentType())
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(content.resource());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoredFileResponse> upload(
            HttpServletRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "EVIDENCE") String purpose
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.store(user.id(), purpose, file));
    }
}
