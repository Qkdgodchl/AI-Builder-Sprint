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
                   c.commitment_type, c.pledge_amount, c.pledge_frequency, c.renewal_due_at,
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
        String intentJson = confirmedIntent(userId, request.consultationId());
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
        createCommitment(userId, applicationId, opportunity, request, intentJson);
        return publicId;
    }

    private void createCommitment(
            Long userId,
            Long applicationId,
            OpportunityResponse opportunity,
            ApplicationCreateRequest request,
            String intentJson
    ) {
        String commitmentPublicId = UUID.randomUUID().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDate effectiveFrom = opportunity.activityStartDateTime() == null
                ? request.participationDate()
                : opportunity.activityStartDateTime().toLocalDate();
        LocalDate effectiveTo = opportunity.activityEndDateTime() == null
                ? effectiveFrom
                : opportunity.activityEndDateTime().toLocalDate();
        Map<String, Object> intent = intentMap(intentJson);
        String commitmentType = string(intent.get("pledgeType"));
        if (commitmentType.isBlank()) commitmentType = opportunity.type();
        java.math.BigDecimal pledgeAmount = decimal(intent.get("amount"));
        String pledgeFrequency = string(intent.get("frequency"));
        LocalDate renewalDueAt = "MONTHLY".equals(pledgeFrequency)
                ? (effectiveFrom == null ? LocalDate.now() : effectiveFrom).plusMonths(1)
                : null;
        String finalCommitmentType = commitmentType;
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO commitments (
                        public_id, application_id, opportunity_id, user_id, organization_id,
                        commitment_status, title, commitment_type, pledge_amount, pledge_frequency,
                        renewal_due_at, intent_snapshot, effective_from, effective_to
                    ) VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, commitmentPublicId);
            statement.setLong(2, applicationId);
            statement.setLong(3, opportunity.id());
            statement.setLong(4, userId);
            statement.setLong(5, opportunity.organizationId());
            statement.setString(6, opportunity.title() + " 참여 약정");
            statement.setString(7, finalCommitmentType);
            statement.setBigDecimal(8, pledgeAmount);
            statement.setString(9, pledgeFrequency.isBlank() ? null : pledgeFrequency);
            statement.setDate(10, renewalDueAt == null ? null : Date.valueOf(renewalDueAt));
            statement.setString(11, intentJson);
            statement.setDate(12, effectiveFrom == null ? null : Date.valueOf(effectiveFrom));
            statement.setDate(13, effectiveTo == null ? null : Date.valueOf(effectiveTo));
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
                request.portraitConsent(),
                intentJson
        );
        saveConsents(
                userId,
                commitmentId,
                request.privacyConsent(),
                request.thirdPartyConsent(),
                request.portraitConsent()
        );
    }

    public void ensureCommitmentExists(
            Long userId,
            String applicationPublicId,
            OpportunityResponse opportunity,
            ApplicationCreateRequest request
    ) {
        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM applications WHERE public_id = ?", Long.class, applicationPublicId);
        if (applicationId == null) return;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM commitments WHERE application_id = ?", Integer.class, applicationId);
        if (count == null || count == 0) {
            String intentJson = json(Map.of());
            createCommitment(userId, applicationId, opportunity, request, intentJson);
        }
    }

    public List<ApplicationResponse> findByUser(Long userId) {
        return jdbcTemplate.query(
                SELECT + " WHERE a.applicant_user_id = ? ORDER BY a.created_at DESC",
                this::map,
                userId
        );
    }

    public List<ApplicationResponse> findAllForOperator() {
        return jdbcTemplate.query(
                SELECT + """
                        ORDER BY org.name, o.title, u.name, a.created_at DESC
                        """,
                this::map
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
                       c.effective_to, c.current_version_no, cv.rendered_content,
                       c.commitment_type, c.pledge_amount, c.pledge_frequency, c.renewal_due_at
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
                rs.getString("rendered_content"),
                rs.getString("commitment_type"),
                rs.getBigDecimal("pledge_amount"),
                rs.getString("pledge_frequency"),
                toLocalDate(rs.getDate("renewal_due_at")),
                renewalStatus(toLocalDate(rs.getDate("renewal_due_at")))
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
                , null
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

    public void renewCommitment(
            String publicId, Long userId, CommitmentRenewalRequest request, boolean termsChanged
    ) {
        Map<String, Object> current = jdbcTemplate.queryForMap("""
                SELECT id, current_version_no, pledge_amount, pledge_frequency, renewal_due_at, effective_to
                FROM commitments WHERE public_id = ? AND user_id = ? AND commitment_status = 'ACTIVE'
                """, publicId, userId);
        Long commitmentId = ((Number) current.get("id")).longValue();
        // 조건이 바뀌면 바뀐 주기를 기준으로 다음 갱신일을 잡아야 한다.
        String frequency = request != null && request.pledgeFrequency() != null
                ? request.pledgeFrequency() : string(current.get("pledge_frequency"));
        LocalDate base = current.get("renewal_due_at") instanceof Date date
                ? date.toLocalDate() : LocalDate.now();
        LocalDate nextDue = switch (frequency) {
            case "MONTHLY" -> base.plusMonths(1);
            case "ANNUAL" -> base.plusYears(1);
            default -> throw new com.pixelcare.global.error.ApiException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "RENEWAL_NOT_APPLICABLE", "정기 약정만 갱신할 수 있습니다.");
        };
        int nextVersion = ((Number) current.get("current_version_no")).intValue() + 1;
        LocalDate effectiveTo = request == null || request.effectiveTo() == null
                ? (current.get("effective_to") instanceof Date date ? date.toLocalDate() : nextDue)
                : request.effectiveTo();

        java.math.BigDecimal nextAmount = request != null && request.pledgeAmount() != null
                ? request.pledgeAmount()
                : (current.get("pledge_amount") instanceof java.math.BigDecimal amount ? amount : null);

        /*
         * 같은 조건이면 기간만 늘리고 바로 유효한 약정으로 둔다.
         * 금액이나 주기가 바뀌면 내용이 달라진 약정이므로 다시 서명을 받아야 한다.
         * SIGNING으로 두면 기존 전자서명 흐름이 그대로 이어받아 서명 후 ACTIVE로 되돌린다.
         */
        String nextStatus = termsChanged ? "SIGNING" : "ACTIVE";
        String changeSummary = termsChanged
                ? "조건 변경 갱신: 금액 %s, 주기 %s, 다음 갱신일 %s (재서명 필요)"
                        .formatted(nextAmount, frequency, nextDue)
                : "정기 약정 갱신: 다음 갱신일 " + nextDue;

        jdbcTemplate.update("""
                UPDATE commitments
                SET renewal_due_at = ?, effective_to = ?, current_version_no = ?,
                    pledge_amount = ?, pledge_frequency = ?,
                    commitment_status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, Date.valueOf(nextDue), Date.valueOf(effectiveTo), nextVersion,
                nextAmount, frequency, nextStatus, commitmentId);
        jdbcTemplate.update("""
                INSERT INTO commitment_versions (
                    commitment_id, version_no, template_version_id, terms_json,
                    rendered_content, change_summary, created_by
                )
                SELECT commitment_id, ?, template_version_id, terms_json,
                       rendered_content, ?, ?
                FROM commitment_versions
                WHERE commitment_id = ? AND version_no = ?
                """, nextVersion, changeSummary, userId, commitmentId, nextVersion - 1);

        Map<String, Object> changes = new java.util.LinkedHashMap<>();
        changes.put("renewalDueAt", nextDue.toString());
        if (termsChanged) {
            changes.put("pledgeAmount", nextAmount == null ? null : nextAmount.toPlainString());
            changes.put("pledgeFrequency", frequency);
            changes.put("resignatureRequired", true);
        }
        jdbcTemplate.update("""
                INSERT INTO commitment_change_requests (
                    commitment_id, requested_by, request_type, requested_changes_json,
                    reason, status, reviewed_by, reviewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                commitmentId, userId,
                termsChanged ? "AMENDMENT" : "RENEWAL",
                json(changes),
                termsChanged ? "사용자 조건 변경 갱신" : "사용자 정기 약정 갱신",
                // 조건이 바뀐 건은 재서명이 끝나야 확정되므로 승인으로 닫지 않는다.
                termsChanged ? "PENDING" : "APPROVED",
                termsChanged ? null : userId);
    }

    private void insertCommitmentVersion(
            Long commitmentId,
            int version,
            Long userId,
            String opportunityTitle,
            String specialConditions,
            boolean privacy,
            boolean thirdParty,
            boolean portrait,
            String intentJson
    ) {
        Map<String, Object> terms = new java.util.LinkedHashMap<>();
        terms.put("specialConditions", specialConditions == null ? "" : specialConditions);
        terms.put("privacyConsent", privacy);
        terms.put("thirdPartyConsent", thirdParty);
        terms.put("portraitConsent", portrait);
        if (intentJson != null) terms.put("confirmedAiIntent", intentMap(intentJson));
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
                ON DUPLICATE KEY UPDATE
                    terms_json = VALUES(terms_json),
                    rendered_content = VALUES(rendered_content),
                    created_by = VALUES(created_by)
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
                        rs.getString("rendered_content"),
                        rs.getString("commitment_type"),
                        rs.getBigDecimal("pledge_amount"),
                        rs.getString("pledge_frequency"),
                        toLocalDate(rs.getDate("renewal_due_at")),
                        renewalStatus(toLocalDate(rs.getDate("renewal_due_at")))
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

    private String confirmedIntent(Long userId, Long consultationId) {
        if (consultationId == null) return null;
        List<String> matches = jdbcTemplate.query("""
                SELECT extracted_preferences_json FROM ai_consultations
                WHERE id = ? AND user_id = ? AND consultation_status = 'CONFIRMED'
                """, (rs, rowNum) -> rs.getString(1), consultationId, userId);
        if (matches.isEmpty()) {
            throw new com.pixelcare.global.error.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "AI_CONSULTATION_NOT_CONFIRMED",
                    "확정된 AI 약정 상담만 신청서에 연결할 수 있습니다."
            );
        }
        return matches.getFirst();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> intentMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try { return objectMapper.readValue(value, Map.class); }
        catch (JsonProcessingException e) { return Map.of(); }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static java.math.BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return new java.math.BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException e) { return null; }
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String renewalStatus(LocalDate dueAt) {
        if (dueAt == null) return "NOT_APPLICABLE";
        return dueAt.isAfter(LocalDate.now()) ? "SCHEDULED" : "DUE";
    }
}
