package com.pixelcare.domain.clm.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Repository
public class WebhookEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public WebhookEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean start(String provider, String eventId, String eventType, String payload) {
        int retry = jdbcTemplate.update("""
                UPDATE processed_webhook_events
                SET event_type = ?, payload_json = ?, processing_status = 'PROCESSING',
                    processed_at = NULL, error_message = NULL
                WHERE provider = ? AND external_event_id = ? AND processing_status = 'FAILED'
                """, eventType, payload, provider, eventId);
        if (retry == 1) return true;
        try {
            return jdbcTemplate.update("""
                INSERT INTO processed_webhook_events (
                    provider, external_event_id, event_type, payload_json, processing_status
                ) SELECT ?, ?, ?, ?, 'PROCESSING'
                WHERE NOT EXISTS (
                    SELECT 1 FROM processed_webhook_events
                    WHERE provider = ? AND external_event_id = ?
                )
                    """, provider, eventId, eventType, payload, provider, eventId) == 1;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    public void complete(String provider, String eventId) {
        jdbcTemplate.update("""
                UPDATE processed_webhook_events
                SET processing_status = 'PROCESSED', processed_at = CURRENT_TIMESTAMP, error_message = NULL
                WHERE provider = ? AND external_event_id = ?
                """, provider, eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String provider, String eventId, String message) {
        jdbcTemplate.update("""
                UPDATE processed_webhook_events
                SET processing_status = 'FAILED', processed_at = CURRENT_TIMESTAMP, error_message = ?
                WHERE provider = ? AND external_event_id = ?
                """, message, provider, eventId);
    }
}
