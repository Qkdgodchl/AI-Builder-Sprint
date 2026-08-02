package com.pixelcare.domain.journal.repository;

import com.pixelcare.domain.journal.dto.ActivityNoteDtos;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ActivityJournalRepository {

    private final JdbcTemplate jdbcTemplate;

    public ActivityJournalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 신청 건의 소유자와 담당 센터를 확인해 기록 권한을 판단한다. */
    public record ApplicationOwner(Long applicationId, Long applicantUserId, Long organizationId,
                                   LocalDate participationDate) {}

    public ApplicationOwner requireApplication(String applicationPublicId) {
        List<ApplicationOwner> rows = jdbcTemplate.query("""
                SELECT a.id, a.applicant_user_id, o.organization_id, a.participation_date
                FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                WHERE a.public_id = ?
                """, (rs, rowNum) -> new ApplicationOwner(
                rs.getLong("id"),
                rs.getLong("applicant_user_id"),
                rs.getLong("organization_id"),
                rs.getDate("participation_date") == null
                        ? null : rs.getDate("participation_date").toLocalDate()
        ), applicationPublicId);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "참여 내역을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    public boolean managesOrganization(Long userId, Long organizationId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM organization_managers
                WHERE user_id = ? AND organization_id = ? AND left_at IS NULL
                """, Integer.class, userId, organizationId);
        return count != null && count > 0;
    }

    public String createNote(
            Long applicationId, String authorType, Long authorUserId,
            LocalDate activityDate, String content, List<Long> fileIds
    ) {
        String publicId = UUID.randomUUID().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO activity_notes (
                        public_id, application_id, author_type, author_user_id,
                        activity_date, content
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, publicId);
            statement.setLong(2, applicationId);
            statement.setString(3, authorType);
            statement.setLong(4, authorUserId);
            statement.setDate(5, Date.valueOf(activityDate));
            statement.setString(6, content);
            return statement;
        }, keyHolder);
        Long noteId = keyHolder.getKey().longValue();

        if (fileIds != null) {
            int order = 0;
            for (Long fileId : fileIds) {
                if (fileId == null) continue;
                jdbcTemplate.update("""
                        INSERT INTO activity_note_files (activity_note_id, stored_file_id, display_order)
                        VALUES (?, ?, ?)
                        """, noteId, fileId, order++);
            }
        }
        return publicId;
    }

    /** 내가 참여한 건과 거기에 달린 기록을 한 번에 모아 캘린더에 뿌린다. */
    public List<ActivityNoteDtos.JournalEntry> findMyJournal(Long userId, LocalDate from, LocalDate to) {
        List<Object[]> rows = jdbcTemplate.query("""
                SELECT a.public_id AS application_public_id, a.status,
                       o.id AS opportunity_id, o.title, org.name AS organization_name,
                       COALESCE(a.participation_date, DATE(o.activity_start_at), DATE(a.submitted_at)) AS activity_date
                FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                JOIN organizations org ON org.id = o.organization_id
                WHERE a.applicant_user_id = ?
                  AND COALESCE(a.participation_date, DATE(o.activity_start_at), DATE(a.submitted_at))
                      BETWEEN ? AND ?
                ORDER BY activity_date DESC
                """, (rs, rowNum) -> new Object[]{
                rs.getString("application_public_id"),
                rs.getString("status"),
                rs.getLong("opportunity_id"),
                rs.getString("title"),
                rs.getString("organization_name"),
                rs.getDate("activity_date").toLocalDate()
        }, userId, Date.valueOf(from), Date.valueOf(to));

        List<ActivityNoteDtos.JournalEntry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            String applicationPublicId = (String) row[0];
            entries.add(new ActivityNoteDtos.JournalEntry(
                    applicationPublicId,
                    (Long) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[1],
                    (LocalDate) row[5],
                    findNotes(applicationPublicId)
            ));
        }
        return entries;
    }

    public List<ActivityNoteDtos.Note> findNotes(String applicationPublicId) {
        Map<String, ActivityNoteDtos.Note> byPublicId = new LinkedHashMap<>();
        Map<String, List<ActivityNoteDtos.NotePhoto>> photos = new LinkedHashMap<>();

        jdbcTemplate.query("""
                SELECT n.public_id, n.author_type, n.activity_date, n.content, n.created_at,
                       u.nickname AS author_name,
                       f.stored_file_id, sf.original_name
                FROM activity_notes n
                JOIN applications a ON a.id = n.application_id
                JOIN users u ON u.id = n.author_user_id
                LEFT JOIN activity_note_files f ON f.activity_note_id = n.id
                LEFT JOIN stored_files sf ON sf.id = f.stored_file_id
                WHERE a.public_id = ? AND n.is_deleted = FALSE
                ORDER BY n.created_at ASC, f.display_order ASC
                """, rs -> {
            String publicId = rs.getString("public_id");
            photos.computeIfAbsent(publicId, key -> new ArrayList<>());
            long fileId = rs.getLong("stored_file_id");
            if (!rs.wasNull()) {
                photos.get(publicId).add(new ActivityNoteDtos.NotePhoto(
                        fileId, rs.getString("original_name")));
            }
            byPublicId.putIfAbsent(publicId, new ActivityNoteDtos.Note(
                    publicId,
                    rs.getString("author_type"),
                    rs.getString("author_name"),
                    rs.getDate("activity_date").toLocalDate(),
                    rs.getString("content"),
                    List.of(),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ));
        }, applicationPublicId);

        List<ActivityNoteDtos.Note> notes = new ArrayList<>();
        byPublicId.forEach((publicId, note) -> notes.add(new ActivityNoteDtos.Note(
                note.publicId(), note.authorType(), note.authorName(),
                note.activityDate(), note.content(),
                photos.getOrDefault(publicId, List.of()), note.createdAt()
        )));
        return notes;
    }
}
