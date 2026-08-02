package com.pixelcare.domain.journal.controller;

import com.pixelcare.domain.journal.dto.ActivityNoteDtos;
import com.pixelcare.domain.journal.repository.ActivityJournalRepository;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ActivityJournalController {

    private final AuthGuard authGuard;
    private final ActivityJournalRepository repository;

    public ActivityJournalController(AuthGuard authGuard, ActivityJournalRepository repository) {
        this.authGuard = authGuard;
        this.repository = repository;
    }

    /** 내 활동 다이어리. 기간을 주지 않으면 최근 1년을 본다. */
    @GetMapping("/me/journal")
    public ApiResponse<List<ActivityNoteDtos.JournalEntry>> myJournal(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        CurrentUser user = authGuard.requireUser(request);
        LocalDate end = to == null ? LocalDate.now().plusMonths(1) : to;
        LocalDate start = from == null ? end.minusYears(1) : from;
        return ApiResponse.success(repository.findMyJournal(user.id(), start, end));
    }

    /** 참여자 본인이 남기는 개인 기록. */
    @PostMapping("/applications/{applicationPublicId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<List<ActivityNoteDtos.Note>> addMyNote(
            HttpServletRequest request,
            @PathVariable String applicationPublicId,
            @Valid @RequestBody ActivityNoteDtos.CreateRequest body
    ) {
        CurrentUser user = authGuard.requireUser(request);
        var application = repository.requireApplication(applicationPublicId);
        if (!application.applicantUserId().equals(user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인 참여 내역에만 기록할 수 있습니다.");
        }
        repository.createNote(application.applicationId(), "USER", user.id(),
                body.activityDate(), body.content(), body.fileIds());
        return ApiResponse.success(repository.findNotes(applicationPublicId));
    }

    /** 담당 센터가 참여자에게 남기는 사진과 코멘트. */
    @PostMapping("/manager/applications/{applicationPublicId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<List<ActivityNoteDtos.Note>> addCenterNote(
            HttpServletRequest request,
            @PathVariable String applicationPublicId,
            @Valid @RequestBody ActivityNoteDtos.CreateRequest body
    ) {
        CurrentUser user = authGuard.requireUser(request);
        var application = repository.requireApplication(applicationPublicId);
        if (!repository.managesOrganization(user.id(), application.organizationId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "담당 센터만 기록을 남길 수 있습니다.");
        }
        repository.createNote(application.applicationId(), "CENTER", user.id(),
                body.activityDate(), body.content(), body.fileIds());
        return ApiResponse.success(repository.findNotes(applicationPublicId));
    }

    /** 참여자와 담당 센터 모두 같은 기록을 본다. */
    @GetMapping("/applications/{applicationPublicId}/notes")
    public ApiResponse<List<ActivityNoteDtos.Note>> notes(
            HttpServletRequest request,
            @PathVariable String applicationPublicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        var application = repository.requireApplication(applicationPublicId);
        boolean allowed = application.applicantUserId().equals(user.id())
                || repository.managesOrganization(user.id(), application.organizationId());
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "기록을 볼 권한이 없습니다.");
        }
        return ApiResponse.success(repository.findNotes(applicationPublicId));
    }
}
