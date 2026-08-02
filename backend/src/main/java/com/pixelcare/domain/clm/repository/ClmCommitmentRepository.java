package com.pixelcare.domain.clm.repository;

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
                    """, Statement.RETURN_GENERATED_KEYS);
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
        return keyHolder.getKey().longValue();
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
                            renewal_due_at = CASE
                                WHEN pledge_frequency = 'MONTHLY' AND renewal_due_at IS NULL
                                THEN DATE_ADD(COALESCE(effective_from, CURRENT_DATE), INTERVAL 1 MONTH)
                                ELSE renewal_due_at END,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND commitment_status <> 'CANCELLED'
                        """, commitmentId);
                jdbcTemplate.update("""
                        UPDATE applications a JOIN commitments c ON c.application_id = a.id
                        SET a.status = 'APPROVED', a.updated_at = CURRENT_TIMESTAMP
                        WHERE c.id = ?
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
