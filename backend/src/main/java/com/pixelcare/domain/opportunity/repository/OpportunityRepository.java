package com.pixelcare.domain.opportunity.repository;

import com.pixelcare.domain.opportunity.dto.OpportunityRequest;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.global.common.KeyExtractUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OpportunityRepository {

    /**
     * 모금액은 별도 컬럼을 갱신하지 않고 체결된 약정 금액을 그때그때 합산한다.
     * 전자서명으로 약정이 확정되는 즉시 진행률에 반영되고, 값이 어긋날 여지가 없다.
     */
    private static final String SELECT = """
            SELECT o.*, org.name AS organization_name,
                   (SELECT COUNT(*) FROM applications a WHERE a.opportunity_id = o.id) AS applicant_count,
                   (SELECT COALESCE(SUM(c.pledge_amount), 0)
                      FROM commitments c
                     WHERE c.opportunity_id = o.id
                       AND c.commitment_status IN ('ACTIVE', 'COMPLETED')) AS pledged_amount
            FROM opportunities o
            JOIN organizations org ON org.id = o.organization_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public OpportunityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchResult searchPublic(
            String type,
            String category,
            String region,
            String keyword,
            int page,
            int size
    ) {
        StringBuilder where = new StringBuilder("""
                WHERE o.status = 'PUBLISHED'
                  AND o.is_deleted = FALSE
                  AND org.verification_status = 'VERIFIED'
                  AND org.is_deleted = FALSE
                """);
        List<Object> args = new ArrayList<>();
        appendFilter(where, args, "o.opportunity_type", type);
        appendFilter(where, args, "o.category", category);
        appendFilter(where, args, "o.region", region);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(o.title) LIKE ? OR LOWER(o.description) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM opportunities o JOIN organizations org ON org.id = o.organization_id "
                        + where,
                Long.class,
                args.toArray()
        );
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(size);
        queryArgs.add(page * size);
        List<OpportunityResponse> items = jdbcTemplate.query(
                SELECT + where + " ORDER BY o.published_at DESC, o.id DESC LIMIT ? OFFSET ?",
                this::map,
                queryArgs.toArray()
        );
        return new SearchResult(items, count == null ? 0 : count);
    }

    public List<OpportunityResponse> findByOrganization(Long organizationId) {
        return jdbcTemplate.query(
                SELECT + """
                        WHERE o.organization_id = ? AND o.is_deleted = FALSE
                        ORDER BY o.created_at DESC
                        """,
                this::map,
                organizationId
        );
    }

    public List<OpportunityResponse> findAllForOperator() {
        return jdbcTemplate.query(
                SELECT + """
                        WHERE o.is_deleted = FALSE
                        ORDER BY o.updated_at DESC, o.created_at DESC
                        """,
                this::map
        );
    }

    public Optional<OpportunityResponse> findPublicById(Long id) {
        return jdbcTemplate.query(
                SELECT + """
                        WHERE o.id = ? AND o.status = 'PUBLISHED' AND o.is_deleted = FALSE
                          AND org.verification_status = 'VERIFIED' AND org.is_deleted = FALSE
                        """,
                this::map,
                id
        ).stream().findFirst();
    }

    public Optional<OpportunityResponse> findById(Long id) {
        return jdbcTemplate.query(
                SELECT + " WHERE o.id = ? AND o.is_deleted = FALSE",
                this::map,
                id
        ).stream().findFirst();
    }

    public Long create(
            Long organizationId,
            Long userId,
            OpportunityRequest request
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO opportunities (
                        organization_id, opportunity_type, category, title, summary, description,
                        region, location, participation_mode, recruitment_start_at,
                        recruitment_end_at, activity_start_at, activity_end_at, capacity,
                        eligibility, target_amount, cancellation_policy, status, created_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?)
                    """, new String[] { "id" });
            int index = 1;
            statement.setLong(index++, organizationId);
            statement.setString(index++, request.type().trim().toUpperCase());
            statement.setString(index++, upperOrNull(request.category()));
            statement.setString(index++, request.title().trim());
            statement.setString(index++, blankToNull(request.summary()));
            statement.setString(index++, request.description().trim());
            statement.setString(index++, upperOrNull(request.region()));
            statement.setString(index++, blankToNull(request.location()));
            statement.setString(index++, defaultParticipationMode(request.participationMode()));
            statement.setTimestamp(index++, timestamp(request.recruitmentStartDateTime()));
            statement.setTimestamp(index++, timestamp(request.recruitmentEndDateTime()));
            statement.setTimestamp(index++, timestamp(request.activityStartDateTime()));
            statement.setTimestamp(index++, timestamp(request.activityEndDateTime()));
            if (request.recruitmentCapacity() == null) statement.setObject(index++, null);
            else statement.setInt(index++, request.recruitmentCapacity());
            statement.setString(index++, blankToNull(request.eligibility()));
            if (request.targetAmount() == null) statement.setObject(index++, null);
            else statement.setLong(index++, request.targetAmount());
            statement.setString(index++, blankToNull(request.cancellationPolicy()));
            statement.setLong(index, userId);
            return statement;
        }, keyHolder);
        Long id = KeyExtractUtils.extractId(keyHolder);
        replaceRequiredDocuments(id, request.requiredDocuments());
        return id;
    }

    public void update(Long id, OpportunityRequest request) {
        jdbcTemplate.update("""
                UPDATE opportunities
                SET opportunity_type = ?, category = ?, title = ?, summary = ?, description = ?,
                    region = ?, location = ?, participation_mode = ?,
                    recruitment_start_at = ?, recruitment_end_at = ?,
                    activity_start_at = ?, activity_end_at = ?, capacity = ?,
                    eligibility = ?, target_amount = ?, cancellation_policy = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """,
                request.type().trim().toUpperCase(),
                upperOrNull(request.category()),
                request.title().trim(),
                blankToNull(request.summary()),
                request.description().trim(),
                upperOrNull(request.region()),
                blankToNull(request.location()),
                defaultParticipationMode(request.participationMode()),
                timestamp(request.recruitmentStartDateTime()),
                timestamp(request.recruitmentEndDateTime()),
                timestamp(request.activityStartDateTime()),
                timestamp(request.activityEndDateTime()),
                request.recruitmentCapacity(),
                blankToNull(request.eligibility()),
                request.targetAmount(),
                blankToNull(request.cancellationPolicy()),
                id
        );
        replaceRequiredDocuments(id, request.requiredDocuments());
    }

    public int softDelete(Long id, String deletedBy) {
        return jdbcTemplate.update("""
                UPDATE opportunities
                SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP,
                    deleted_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """, deletedBy, id);
    }

    public boolean isCreatedBy(Long id, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM opportunities
                WHERE id = ? AND created_by = ? AND is_deleted = FALSE
                """, Integer.class, id, userId);
        return count != null && count > 0;
    }

    public int transitionStatus(Long id, List<String> from, String to) {
        String placeholders = String.join(",", java.util.Collections.nCopies(from.size(), "?"));
        List<Object> args = new ArrayList<>();
        if ("PUBLISHED".equals(to)) {
            args.add(to);
        } else {
            args.add(to);
        }
        args.add(id);
        args.addAll(from);
        String published = "PUBLISHED".equals(to)
                ? ", published_at = CURRENT_TIMESTAMP\n"
                : "";
        return jdbcTemplate.update("""
                UPDATE opportunities SET status = ?, updated_at = CURRENT_TIMESTAMP
                """ + published + """
                WHERE id = ? AND is_deleted = FALSE AND status IN (
                """ + placeholders + ")", args.toArray());
    }

    private void replaceRequiredDocuments(
            Long opportunityId,
            List<OpportunityRequest.RequiredDocumentRequest> documents
    ) {
        jdbcTemplate.update(
                "DELETE FROM opportunity_required_documents WHERE opportunity_id = ?",
                opportunityId
        );
        if (documents == null) {
            return;
        }
        for (int i = 0; i < documents.size(); i++) {
            OpportunityRequest.RequiredDocumentRequest document = documents.get(i);
            jdbcTemplate.update("""
                    INSERT INTO opportunity_required_documents (
                        opportunity_id, document_code, document_name,
                        description, is_required, display_order
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    opportunityId,
                    document.code().trim().toUpperCase(),
                    document.name().trim(),
                    blankToNull(document.description()),
                    document.required() == null || document.required(),
                    i
            );
        }
    }

    private List<OpportunityResponse.RequiredDocumentResponse> requiredDocuments(Long opportunityId) {
        return jdbcTemplate.query("""
                SELECT document_code, document_name, description, is_required, display_order
                FROM opportunity_required_documents
                WHERE opportunity_id = ?
                ORDER BY display_order, id
                """, (rs, rowNum) -> new OpportunityResponse.RequiredDocumentResponse(
                rs.getString("document_code"),
                rs.getString("document_name"),
                rs.getString("description"),
                rs.getBoolean("is_required"),
                rs.getInt("display_order")
        ), opportunityId);
    }

    private OpportunityResponse map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Long id = rs.getLong("id");
        return new OpportunityResponse(
                id,
                rs.getLong("organization_id"),
                rs.getString("organization_name"),
                rs.getString("opportunity_type"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("description"),
                rs.getString("region"),
                rs.getString("location"),
                rs.getString("participation_mode"),
                nullableInteger(rs.getObject("capacity")),
                localDateTime(rs.getTimestamp("recruitment_start_at")),
                localDateTime(rs.getTimestamp("recruitment_end_at")),
                localDateTime(rs.getTimestamp("activity_start_at")),
                localDateTime(rs.getTimestamp("activity_end_at")),
                rs.getString("eligibility"),
                nullableLong(rs.getObject("target_amount")),
                rs.getLong("pledged_amount"),
                rs.getString("cancellation_policy"),
                rs.getString("status"),
                rs.getLong("applicant_count"),
                requiredDocuments(id),
                nullableLong(rs.getObject("created_by")),
                localDateTime(rs.getTimestamp("published_at")),
                localDateTime(rs.getTimestamp("created_at"))
        );
    }

    private static void appendFilter(
            StringBuilder sql,
            List<Object> args,
            String column,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            sql.append(" AND ").append(column).append(" = ?");
            args.add(value.trim().toUpperCase());
        }
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static String defaultParticipationMode(String value) {
        String normalized = upperOrNull(value);
        return normalized == null ? "OFFLINE" : normalized;
    }

    public record SearchResult(List<OpportunityResponse> items, long totalElements) {}
}
