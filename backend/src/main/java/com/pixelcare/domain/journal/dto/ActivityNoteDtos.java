package com.pixelcare.domain.journal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ActivityNoteDtos {

    private ActivityNoteDtos() {}

    /** 사진은 먼저 업로드한 뒤 파일 id 목록으로 붙인다. */
    public record CreateRequest(
            @NotNull LocalDate activityDate,
            String content,
            List<Long> fileIds
    ) {}

    public record NotePhoto(Long fileId, String originalName) {}

    public record Note(
            String publicId,
            String authorType,
            String authorName,
            LocalDate activityDate,
            String content,
            List<NotePhoto> photos,
            LocalDateTime createdAt
    ) {}

    /** 캘린더 한 칸에 해당하는 참여 기록. */
    public record JournalEntry(
            String applicationPublicId,
            Long opportunityId,
            String opportunityTitle,
            String organizationName,
            String status,
            LocalDate activityDate,
            List<Note> notes
    ) {}
}
