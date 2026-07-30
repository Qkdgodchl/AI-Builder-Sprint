package com.pixelcare.domain.application.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.application.dto.*;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ApplicationRepository {

    private static final String SELECT = """
            SELECT a.*, o.title AS opportunity_title, o.opportunity_type,
                   o.organization_id, org.name AS organization_name,
                   u.name AS applicant_name, u.email AS applicant_email,
                   c.public_id AS commitment_public_id,
                   c.commitment_status, c.title AS commitment_title,
                   c.effective_from, c.effective_to, c.current_version_no,
                   cv.rendered_content
            FROM applications a
            JOIN opportunities o ON o.id = a.opportunity_id
            JOIN organizations org ON org.id = o.organization_id
            JOIN users u ON u.id = a.applicant_user_id
            LEFT JOIN commitments c ON c.application_id = a.id
            LEFT JOIN commitment_versions cv
              ON cv.commitment_id = c.id AND cv.version_no = c.current_version_no
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ApplicationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean exists(Long opportunityId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM applications
                WHERE opportunity_id = ? AND applicant_user_id = ?
                  AND status <> 'CANCELLED'
                """, Integer.class, opportunityId, userId);
        return count != null && count > 0;
    }

    public long activeApplicationCount(Long opportunityId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM applications
                WHERE opportunity_id = ? AND status NOT IN ('CANCELLED', 'REJECTED')
                """, Long.class, opportunityId);
        return count == null ? 0 : count;
    }

    public String create(
            Long userId,
            OpportunityResponse opportunity,
            ApplicationCreateRequest request
    ) {
        String publicId = UUID.randomUUID().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String answersJson = json(Map.of(
                "specialConditions", request.specialConditions() == null ? "" : request.specialConditions()
        ));
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO applications (
                        public_id, opportunity_id, applicant_user_id, consultation_id,
                        participation_date, special_conditions, answers_json,
                        privacy_consent, third_party_consent, portrait_consent,
                        status, submitted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'APPLIED', CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, publicId);
            statement.setLong(2, opportunity.id());
            statement.setLong(3, userId);
            if (request.consultationId() == null) statement.setObject(4, null);
            else statement.setLong(4, request.consultationId());
            statement.setDate(5, request.participationDate() == null
                    ? null : Date.valueOf(request.participationDate()));
            statement.setString(6, request.specialConditions());
            statement.setString(7, answersJson);
            statement.setBoolean(8, request.privacyConsent());
            statement.setBoolean(9, request.thirdPartyConsent());
            statement.setBoolean(10, request.portraitConsent());
            return statement;
        }, keyHolder);
        Long applicationId = keyHolder.getKey().longValue();
        createCommitment(userId, applicationId, opportunity, request);
        return publicId;
    }

    private void createCommitment(
            Long userId,
            Long applicationId,
            OpportunityResponse opportunity,
            ApplicationCreateRequest request
    ) {
        String commitmentPublicId = UUID.randomUUID().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDate effectiveFrom = opportunity.activityStartDateTime() == null
                ? request.participationDate()
                : opportunity.activityStartDateTime().toLocalDate();
        LocalDate effectiveTo = opportunity.activityEndDateTime() == null
                ? effectiveFrom
                : opportunity.activityEndDateTime().toLocalDate();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO commitments (
                        public_id, application_id, opportunity_id, user_id, organization_id,
                        commitment_status, title, effective_from, effective_to
                    ) VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, commitmentPublicId);
            statement.setLong(2, applicationId);
            statement.setLong(3, opportunity.id());
            statement.setLong(4, userId);
            statement.setLong(5, opportunity.organizationId());
            statement.setString(6, opportunity.title() + " 참여 약정");
            statement.setDate(7, effectiveFrom == null ? null : Date.valueOf(effectiveFrom));
            statement.setDate(8, effectiveTo == null ? null : Date.valueOf(effectiveTo));
            return statement;
        }, keyHolder);
        Long commitmentId = keyHolder.getKey().longValue();
        insertCommitmentVersion(
                commitmentId,
                1,
                userId,
                opportunity.title(),
                request.specialConditions(),
                request.privacyConsent(),
                request.thirdPartyConsent(),
                request.portraitConsent()
        );
        saveConsents(
                userId,
                commitmentId,
                request.privacyConsent(),
                request.thirdPartyConsent(),
                request.portraitConsent()
        );
    }

    public List<ApplicationResponse> findByUser(Long userId) {
        return jdbcTemplate.query(
                SELECT + " WHERE a.applicant_user_id = ? ORDER BY a.created_at DESC",
                this::map,
                userId
        );
    }

    public List<ApplicationResponse> findByOpportunity(Long opportunityId) {
        return jdbcTemplate.query(
                SELECT + " WHERE a.opportunity_id = ? ORDER BY a.created_at DESC",
                this::map,
                opportunityId
        );
    }

    public Optional<ApplicationResponse> findByPublicId(String publicId) {
        return jdbcTemplate.query(
                SELECT + " WHERE a.public_id = ?",
                this::map,
                publicId
        ).stream().findFirst();
    }

    public boolean managerCanAccess(Long managerId, String applicationPublicId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                JOIN organization_managers om ON om.organization_id = o.organization_id
                WHERE a.public_id = ? AND om.user_id = ? AND om.left_at IS NULL
                """, Integer.class, applicationPublicId, managerId);
        return count != null && count > 0;
    }

    public void cancel(String publicId, Long userId) {
        jdbcTemplate.update("""
                UPDATE applications
                SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ? AND applicant_user_id = ?
                """, publicId, userId);
        jdbcTemplate.update("""
                UPDATE commitments c
                JOIN applications a ON a.id = c.application_id
                SET c.commitment_status = 'CANCELLED',
                    c.cancelled_at = CURRENT_TIMESTAMP,
                    c.updated_at = CURRENT_TIMESTAMP
                WHERE a.public_id = ?
                """, publicId);
    }

    public void decide(String publicId, Long managerId, String status, String reason) {
        jdbcTemplate.update("""
                UPDATE applications
                SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP,
                    rejection_reason = ?, updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ?
                """, status, managerId, reason, publicId);
        String commitmentStatus = switch (status) {
            case "APPROVED" -> "APPROVED";
            case "REVISION_REQUESTED" -> "REVISION_REQUESTED";
            case "REJECTED" -> "CANCELLED";
            default -> "IN_REVIEW";
        };
        jdbcTemplate.update("""
                UPDATE commitments c
                JOIN applications a ON a.id = c.application_id
                SET c.commitment_status = ?, c.updated_at = CURRENT_TIMESTAMP
                WHERE a.public_id = ?
                """, commitmentStatus, publicId);
    }

    public Optional<ApplicationResponse.CommitmentSummary> findCommitment(String commitmentPublicId) {
        List<ApplicationResponse.CommitmentSummary> result = jdbcTemplate.query("""
                SELECT c.public_id, c.commitment_status, c.title, c.effective_from,
                       c.effective_to, c.current_version_no, cv.rendered_content
                FROM commitments c
                JOIN commitment_versions cv
                  ON cv.commitment_id = c.id AND cv.version_no = c.current_version_no
                WHERE c.public_id = ?
                """, (rs, rowNum) -> new ApplicationResponse.CommitmentSummary(
                rs.getString("public_id"),
                rs.getString("commitment_status"),
                rs.getString("title"),
                toLocalDate(rs.getDate("effective_from")),
                toLocalDate(rs.getDate("effective_to")),
                rs.getInt("current_version_no"),
                rs.getString("rendered_content")
        ), commitmentPublicId);
        return result.stream().findFirst();
    }

    public boolean ownsCommitment(Long userId, String commitmentPublicId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM commitments
                WHERE public_id = ? AND user_id = ?
                """, Integer.class, commitmentPublicId, userId);
        return count != null && count > 0;
    }

    public void updateCommitment(
            String publicId,
            Long userId,
            CommitmentUpdateRequest request
    ) {
        Map<String, Object> current = jdbcTemplate.queryForMap("""
                SELECT c.id, c.current_version_no, c.title, o.title AS opportunity_title
                FROM commitments c
                JOIN opportunities o ON o.id = c.opportunity_id
                WHERE c.public_id = ?
                """, publicId);
        Long commitmentId = ((Number) current.get("id")).longValue();
        int nextVersion = ((Number) current.get("current_version_no")).intValue() + 1;
        jdbcTemplate.update("""
                UPDATE commitments
                SET effective_from = COALESCE(?, effective_from),
                    effective_to = COALESCE(?, effective_to),
                    current_version_no = ?,
                    commitment_status = 'DRAFT',
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                request.effectiveFrom() == null ? null : Date.valueOf(request.effectiveFrom()),
                request.effectiveTo() == null ? null : Date.valueOf(request.effectiveTo()),
                nextVersion,
                commitmentId
        );
        boolean privacy = Boolean.TRUE.equals(request.privacyConsent());
        boolean thirdParty = Boolean.TRUE.equals(request.thirdPartyConsent());
        boolean portrait = Boolean.TRUE.equals(request.portraitConsent());
        insertCommitmentVersion(
                commitmentId,
                nextVersion,
                userId,
                String.valueOf(current.get("opportunity_title")),
                request.specialConditions(),
                privacy,
                thirdParty,
                portrait
        );
        saveConsents(userId, commitmentId, privacy, thirdParty, portrait);
    }

    public void submitCommitment(String publicId) {
        jdbcTemplate.update("""
                UPDATE commitments
                SET commitment_status = 'IN_REVIEW', updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ? AND commitment_status IN ('DRAFT', 'REVISION_REQUESTED')
                """, publicId);
        jdbcTemplate.update("""
                UPDATE applications a
                JOIN commitments c ON c.application_id = a.id
                SET a.status = 'IN_REVIEW', a.updated_at = CURRENT_TIMESTAMP
                WHERE c.public_id = ?
                """, publicId);
    }

    private void insertCommitmentVersion(
            Long commitmentId,
            int version,
            Long userId,
            String opportunityTitle,
            String specialConditions,
            boolean privacy,
            boolean thirdParty,
            boolean portrait
    ) {
        Map<String, Object> terms = Map.of(
                "specialConditions", specialConditions == null ? "" : specialConditions,
                "privacyConsent", privacy,
                "thirdPartyConsent", thirdParty,
                "portraitConsent", portrait
        );
        String rendered = """
                [참여 약정서]
                프로그램: %s
                특별 조건: %s
                개인정보 수집 동의: %s
                제3자 제공 동의: %s
                초상권 동의: %s
                """.formatted(
                opportunityTitle,
                specialConditions == null ? "없음" : specialConditions,
                privacy ? "동의" : "미동의",
                thirdParty ? "동의" : "미동의",
                portrait ? "동의" : "미동의"
        );
        jdbcTemplate.update("""
                INSERT INTO commitment_versions (
                    commitment_id, version_no, terms_json, rendered_content, created_by
                ) VALUES (?, ?, ?, ?, ?)
                """, commitmentId, version, json(terms), rendered, userId);
    }

    private void saveConsents(
            Long userId,
            Long commitmentId,
            boolean privacy,
            boolean thirdParty,
            boolean portrait
    ) {
        saveConsent(userId, commitmentId, "PRIVACY_COLLECTION", privacy);
        saveConsent(userId, commitmentId, "THIRD_PARTY_PROVISION", thirdParty);
        saveConsent(userId, commitmentId, "PORTRAIT_RIGHTS", portrait);
    }

    private void saveConsent(Long userId, Long commitmentId, String type, boolean agreed) {
        jdbcTemplate.update("""
                INSERT INTO consents (
                    user_id, commitment_id, consent_type, policy_version,
                    is_agreed, agreed_at
                ) VALUES (?, ?, ?, '2026-07-30', ?, ?)
                """,
                userId,
                commitmentId,
                type,
                agreed,
                agreed ? Timestamp.valueOf(LocalDateTime.now()) : null
        );
    }

    private ApplicationResponse map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String publicId = rs.getString("public_id");
        String commitmentPublicId = rs.getString("commitment_public_id");
        ApplicationResponse.CommitmentSummary commitment = commitmentPublicId == null
                ? null
                : new ApplicationResponse.CommitmentSummary(
                        commitmentPublicId,
                        rs.getString("commitment_status"),
                        rs.getString("commitment_title"),
                        toLocalDate(rs.getDate("effective_from")),
                        toLocalDate(rs.getDate("effective_to")),
                        rs.getInt("current_version_no"),
                        rs.getString("rendered_content")
                );
        return new ApplicationResponse(
                publicId,
                rs.getLong("opportunity_id"),
                rs.getString("opportunity_title"),
                rs.getString("opportunity_type"),
                rs.getLong("organization_id"),
                rs.getString("organization_name"),
                rs.getLong("applicant_user_id"),
                rs.getString("applicant_name"),
                rs.getString("applicant_email"),
                toLocalDate(rs.getDate("participation_date")),
                rs.getString("special_conditions"),
                rs.getBoolean("privacy_consent"),
                rs.getBoolean("third_party_consent"),
                rs.getBoolean("portrait_consent"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("submitted_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at")),
                rs.getString("rejection_reason"),
                commitment,
                requiredDocuments(rs.getLong("opportunity_id"), commitment == null ? null : commitment.status()),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private List<ApplicationResponse.DocumentSummary> requiredDocuments(
            Long opportunityId,
            String commitmentStatus
    ) {
        return jdbcTemplate.query("""
                SELECT document_code, document_name, description, is_required
                FROM opportunity_required_documents
                WHERE opportunity_id = ?
                ORDER BY display_order, id
                """, (rs, rowNum) -> new ApplicationResponse.DocumentSummary(
                rs.getString("document_code"),
                rs.getString("document_name"),
                rs.getString("description"),
                rs.getBoolean("is_required"),
                commitmentStatus == null ? "작성 전" : commitmentStatus
        ), opportunityId);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("신청 데이터를 JSON으로 변환할 수 없습니다.", e);
        }
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
