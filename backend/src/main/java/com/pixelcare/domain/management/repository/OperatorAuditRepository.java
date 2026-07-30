package com.pixelcare.domain.management.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperatorAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperatorAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(Long operatorId, String action, String targetType, String targetId) {
        jdbcTemplate.update("""
                INSERT INTO admin_audit_logs (
                    actor_user_id, action_type, target_type, target_id
                ) VALUES (?, ?, ?, ?)
                """, operatorId, action, targetType, targetId);
    }
}
