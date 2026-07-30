package com.pixelcare.domain.file.controller;

import com.pixelcare.domain.file.dto.StoredFileResponse;
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

    public StoredFileController(AuthGuard authGuard, StoredFileService service) {
        this.authGuard = authGuard;
        this.service = service;
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
