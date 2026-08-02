package com.pixelcare.domain.ai.repository;

import com.pixelcare.global.common.KeyExtractUtils;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Optional;

@Repository
public class AiConsultationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AiConsultationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_consultations (user_id, title, consultation_status, started_at)
                    VALUES (?, 'AI 약정 의사 정리', 'IN_PROGRESS', CURRENT_TIMESTAMP)
                    """, new String[] { "id" });
            statement.setLong(1, userId);
            return statement;
        }, keyHolder);
        return KeyExtractUtils.extractId(keyHolder);
    }

    public Optional<Map<String, Object>> findOwned(Long id, Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT id, user_id, consultation_status, intent_summary, extracted_preferences_json
                FROM ai_consultations
                WHERE id = ? AND user_id = ?
                """, id, userId).stream().findFirst();
    }

    public void addMessage(Long consultationId, String sender, String content, String metadataJson) {
        Integer next = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sequence_no), 0) + 1 FROM ai_messages WHERE consultation_id = ?
                """, Integer.class, consultationId);
        jdbcTemplate.update("""
                INSERT INTO ai_messages (consultation_id, sender_type, content, metadata_json, sequence_no)
                VALUES (?, ?, ?, ?, ?)
                """, consultationId, sender, content, metadataJson, next == null ? 1 : next);
    }

    public void recordExternalAiConsent(Long id, Long userId) {
        int changed = jdbcTemplate.update("""
                UPDATE ai_consultations
                SET external_ai_consent_at = COALESCE(external_ai_consent_at, CURRENT_TIMESTAMP),
                    external_ai_provider = 'UPSTAGE_SOLAR',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, id, userId);
        if (changed == 0) throw notFound();
    }

    public void updateIntent(Long id, Long userId, String summary, String intentJson) {
        int changed = jdbcTemplate.update("""
                UPDATE ai_consultations
                SET intent_summary = ?, extracted_preferences_json = ?,
                    consultation_status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, summary, intentJson, id, userId);
        if (changed == 0) throw notFound();
    }

    public void confirm(Long id, Long userId) {
        int changed = jdbcTemplate.update("""
                UPDATE ai_consultations
                SET consultation_status = 'CONFIRMED', completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND extracted_preferences_json IS NOT NULL
                """, id, userId);
        if (changed == 0) throw notFound();
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONSULTATION_NOT_FOUND", "AI 약정 상담을 찾을 수 없습니다.");
    }

    /**
     * 특정 상담의 메시지 이력을 순서대로 반환합니다.
     * 반환 형식: List of [senderType, content]
     */
    public java.util.List<String[]> findMessages(Long consultationId) {
        return jdbcTemplate.query("""
                SELECT sender_type, content FROM ai_messages
                WHERE consultation_id = ?
                ORDER BY sequence_no ASC
                """,
                (rs, rowNum) -> new String[]{ rs.getString("sender_type"), rs.getString("content") },
                consultationId
        );
    }
}
