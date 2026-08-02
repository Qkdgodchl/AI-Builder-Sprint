package com.pixelcare.domain.clm.repository;

import com.pixelcare.global.common.KeyExtractUtils;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ClmCommitmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClmCommitmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CommitmentSigningContext requireOwnedSigningContext(String publicId, Long userId) {
        List<CommitmentSigningContext> rows = jdbcTemplate.query("""
                SELECT c.id, c.public_id, c.opportunity_id, c.title, c.commitment_status,
                       c.pledge_frequency, c.effective_from,
                       u.name AS applicant_name, u.email AS applicant_email,
                       o.opportunity_type, COALESCE(org.name, '픽셀케어 지정 기관') AS organizer
                FROM commitments c
                JOIN users u ON u.id = c.user_id
                JOIN opportunities o ON o.id = c.opportunity_id
                LEFT JOIN organizations org ON org.id = o.organization_id
                WHERE c.public_id = ? AND c.user_id = ?
                """, (rs, rowNum) -> new CommitmentSigningContext(
                rs.getLong("id"), rs.getString("public_id"), rs.getLong("opportunity_id"),
                rs.getString("title"), rs.getString("commitment_status"),
                rs.getString("pledge_frequency"),
                rs.getDate("effective_from") == null ? null : rs.getDate("effective_from").toLocalDate(),
                rs.getString("applicant_name"), rs.getString("applicant_email"),
                rs.getString("opportunity_type"), rs.getString("organizer")
        ), publicId, userId);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMITMENT_NOT_FOUND", "서명할 약정서를 찾을 수 없습니다.");
        }
        CommitmentSigningContext context = rows.getFirst();
        if (List.of("CANCELLED", "REJECTED").contains(context.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "COMMITMENT_NOT_READY_FOR_SIGNING",
                    "취소되거나 거절된 약정서에는 서명을 요청할 수 없습니다.");
        }
        return context;
    }

    public Long createSignatureRequest(CommitmentSigningContext context, Long userId,
                                       String email, String providerRequestId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO signature_requests (
                        public_id, commitment_id, provider, provider_request_id,
                        signer_user_id, signer_email, signature_status
                    ) VALUES (?, ?, 'MODUSIGN', ?, ?, ?, 'REQUESTED')
                    """, new String[] { "id" });
            statement.setString(1, UUID.randomUUID().toString());
            statement.setLong(2, context.id());
            statement.setString(3, providerRequestId);
            statement.setLong(4, userId);
            statement.setString(5, email);
            return statement;
        }, keyHolder);
        jdbcTemplate.update("""
                UPDATE commitments SET commitment_status = 'SIGNING', updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, context.id());
        return KeyExtractUtils.extractId(keyHolder);
    }

    public void applySignatureState(Long commitmentId, Long signatureRequestId, String state) {
        if (commitmentId == null || signatureRequestId == null) return;
        switch (state) {
            case "SIGNED" -> {
                jdbcTemplate.update("""
                        UPDATE signature_requests
                        SET signature_status = 'SIGNED', signed_at = COALESCE(signed_at, CURRENT_TIMESTAMP)
                        WHERE id = ? AND signature_status <> 'SIGNED'
                        """, signatureRequestId);
                jdbcTemplate.update("""
                        UPDATE commitments
                        SET commitment_status = 'ACTIVE', signed_at = COALESCE(signed_at, CURRENT_TIMESTAMP),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND commitment_status <> 'CANCELLED'
                        """, commitmentId);
                List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT pledge_frequency, renewal_due_at, effective_from FROM commitments WHERE id = ?", commitmentId);
                if (!rows.isEmpty()) {
                    String freq = (String) rows.get(0).get("pledge_frequency");
                    Object due = rows.get(0).get("renewal_due_at");
                    Object eff = rows.get(0).get("effective_from");
                    if ("MONTHLY".equals(freq) && due == null) {
                        java.time.LocalDate effDate = eff instanceof java.sql.Date d ? d.toLocalDate() : java.time.LocalDate.now();
                        jdbcTemplate.update("UPDATE commitments SET renewal_due_at = ? WHERE id = ?",
                                java.sql.Date.valueOf(effDate.plusMonths(1)), commitmentId);
                    }
                }
                jdbcTemplate.update("""
                        UPDATE applications
                        SET status = 'APPROVED', updated_at = CURRENT_TIMESTAMP
                        WHERE id = (SELECT application_id FROM commitments WHERE id = ?)
                        """, commitmentId);
            }
            case "REJECTED", "CANCELED" -> {
                jdbcTemplate.update("""
                        UPDATE signature_requests
                        SET signature_status = ?, failed_reason = ?
                        WHERE id = ? AND signature_status <> 'SIGNED'
                        """, state, "모두싸인 이벤트: " + state, signatureRequestId);
                jdbcTemplate.update("""
                        UPDATE commitments SET commitment_status = 'SIGNATURE_FAILED', updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND commitment_status NOT IN ('ACTIVE', 'COMPLETED', 'CANCELLED')
                        """, commitmentId);
            }
            default -> jdbcTemplate.update("""
                    UPDATE signature_requests SET signature_status = ?
                    WHERE id = ? AND signature_status NOT IN ('SIGNED', 'REJECTED', 'CANCELED')
                    """, state, signatureRequestId);
        }
    }

    public Long findConsultationIdByCommitmentId(Long commitmentId) {
        List<Long> list = jdbcTemplate.query("""
                SELECT a.consultation_id
                FROM commitments c
                JOIN applications a ON a.id = c.application_id
                WHERE c.id = ? AND a.consultation_id IS NOT NULL
                """, (rs, rowNum) -> rs.getLong("consultation_id"), commitmentId);
        return list.isEmpty() ? null : list.getFirst();
    }

    /** 서명 완료 감사 메시지를 만들 때 쓰는 약정 요약. 약정이 없으면 비어 있다. */
    public java.util.Optional<GratitudeContext> findGratitudeContext(Long commitmentId) {
        if (commitmentId == null) return java.util.Optional.empty();
        List<GratitudeContext> rows = jdbcTemplate.query("""
                SELECT u.name AS applicant_name, c.title, c.commitment_type,
                       c.pledge_amount, c.pledge_frequency, o.name AS organization_name
                FROM commitments c
                JOIN users u ON u.id = c.user_id
                JOIN organizations o ON o.id = c.organization_id
                WHERE c.id = ?
                """, (rs, rowNum) -> new GratitudeContext(
                rs.getString("applicant_name"),
                rs.getString("title"),
                rs.getString("commitment_type"),
                rs.getBigDecimal("pledge_amount"),
                rs.getString("pledge_frequency"),
                rs.getString("organization_name")
        ), commitmentId);
        return rows.stream().findFirst();
    }

    public record GratitudeContext(
            String applicantName,
            String title,
            String commitmentType,
            java.math.BigDecimal pledgeAmount,
            String pledgeFrequency,
            String organizationName
    ) {}
    public record CommitmentSigningContext(
            Long id,
            String publicId,
            Long opportunityId,
            String title,
            String status,
            String pledgeFrequency,
            LocalDate effectiveFrom,
            String applicantName,
            String applicantEmail,
            String opportunityType,
            String organizer
    ) {}
}
