package com.pixelcare.domain.management.repository;

import com.pixelcare.domain.management.dto.*;
import com.pixelcare.global.common.KeyExtractUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ManagementRepository {

    private final JdbcTemplate jdbcTemplate;

    public ManagementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasPendingManagerApplication(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM manager_applications
                WHERE applicant_user_id = ? AND status = 'PENDING'
                """, Integer.class, userId);
        return count != null && count > 0;
    }

    public String createManagerApplication(Long userId, ManagerApplicationRequest request) {
        String publicId = java.util.UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO manager_applications (
                    public_id, applicant_user_id, organization_name, position, contact,
                    organization_type, business_registration_number, planned_center_name,
                    proof_file_id, reason, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """,
                publicId,
                userId,
                request.organizationName().trim(),
                request.position().trim(),
                request.contact().trim(),
                request.organizationType().trim().toUpperCase(),
                blankToNull(request.registrationNumber()),
                blankToNull(request.plannedCenterName()),
                request.evidenceFileId(),
                request.reason().trim()
        );
        List<Long> evidenceFileIds = request.evidenceFileIds() == null
                ? List.of()
                : request.evidenceFileIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (request.evidenceFileId() != null && !evidenceFileIds.contains(request.evidenceFileId())) {
            evidenceFileIds = java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(request.evidenceFileId()),
                    evidenceFileIds.stream()
            ).distinct().toList();
        }
        for (Long fileId : evidenceFileIds) {
            jdbcTemplate.update("""
                    INSERT INTO manager_application_files (
                        manager_application_id, file_id, document_type
                    )
                    SELECT id, ?, 'EVIDENCE'
                    FROM manager_applications
                    WHERE public_id = ?
                    """, fileId, publicId);
        }
        return publicId;
    }

    public List<ManagerApplicationResponse> findManagerApplicationsByUser(Long userId) {
        return queryManagerApplications(
                "WHERE ma.applicant_user_id = ? ORDER BY ma.created_at DESC",
                userId
        );
    }

    public List<ManagerApplicationResponse> findManagerApplications(String status) {
        if (status == null || status.isBlank()) {
            return queryManagerApplications("ORDER BY ma.created_at DESC");
        }
        return queryManagerApplications(
                "WHERE ma.status = ? ORDER BY ma.created_at DESC",
                status.toUpperCase()
        );
    }

    public Optional<ManagerApplicationResponse> findManagerApplication(String publicId) {
        return queryManagerApplications("WHERE ma.public_id = ?", publicId).stream().findFirst();
    }

    public void cancelManagerApplication(String publicId, Long userId) {
        jdbcTemplate.update("""
                UPDATE manager_applications
                SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ? AND applicant_user_id = ? AND status = 'PENDING'
                """, publicId, userId);
    }

    public void decideManagerApplication(
            String publicId,
            String status,
            Long operatorId,
            String reason
    ) {
        jdbcTemplate.update("""
                UPDATE manager_applications
                SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP,
                    rejection_reason = ?, updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ? AND status = 'PENDING'
                """, status, operatorId, reason, publicId);
    }

    public boolean hasPendingOrganizationApplication(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM organization_applications
                WHERE applicant_user_id = ? AND status = 'PENDING'
                """, Integer.class, userId);
        return count != null && count > 0;
    }

    public String createOrganizationApplication(Long userId, OrganizationApplicationRequest request) {
        String publicId = java.util.UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO organization_applications (
                    public_id, applicant_user_id, name, organization_type,
                    registration_number, representative_name, phone, email,
                    address, description, proof_file_id, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """,
                publicId,
                userId,
                request.name().trim(),
                request.organizationType().trim().toUpperCase(),
                blankToNull(request.registrationNumber()),
                request.representativeName().trim(),
                blankToNull(request.phone()),
                blankToNull(request.email()),
                blankToNull(request.address()),
                blankToNull(request.description()),
                request.evidenceFileId()
        );
        return publicId;
    }

    public List<OrganizationApplicationResponse> findOrganizationApplicationsByUser(Long userId) {
        return queryOrganizationApplications(
                "WHERE oa.applicant_user_id = ? ORDER BY oa.created_at DESC",
                userId
        );
    }

    public List<OrganizationApplicationResponse> findOrganizationApplications(String status) {
        if (status == null || status.isBlank()) {
            return queryOrganizationApplications("ORDER BY oa.created_at DESC");
        }
        return queryOrganizationApplications(
                "WHERE oa.status = ? ORDER BY oa.created_at DESC",
                status.toUpperCase()
        );
    }

    public Optional<OrganizationApplicationResponse> findOrganizationApplication(String publicId) {
        return queryOrganizationApplications("WHERE oa.public_id = ?", publicId).stream().findFirst();
    }

    public void decideOrganizationApplication(
            String publicId,
            String status,
            Long operatorId,
            String reason
    ) {
        jdbcTemplate.update("""
                UPDATE organization_applications
                SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP,
                    rejection_reason = ?, updated_at = CURRENT_TIMESTAMP
                WHERE public_id = ? AND status = 'PENDING'
                """, status, operatorId, reason, publicId);
    }

    public Long createOrganizationForApprovedApplication(OrganizationApplicationResponse application) {
        List<Long> existing = jdbcTemplate.query("""
                SELECT o.id
                FROM organizations o
                JOIN organization_applications oa ON oa.id = o.source_application_id
                WHERE oa.public_id = ? AND o.is_deleted = FALSE
                """, (rs, rowNum) -> rs.getLong(1), application.publicId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO organizations (
                        source_application_id, name, organization_type, registration_number,
                        representative_name, phone, email, address, description,
                        verification_status, created_at
                    )
                    SELECT id, ?, ?, ?, ?, ?, ?, ?, ?, 'VERIFIED', CURRENT_TIMESTAMP
                    FROM organization_applications
                    WHERE public_id = ?
                    """, new String[] { "id" });
            statement.setString(1, application.name());
            statement.setString(2, application.organizationType());
            statement.setString(3, application.registrationNumber());
            statement.setString(4, application.representativeName());
            statement.setString(5, application.phone());
            statement.setString(6, application.email());
            statement.setString(7, application.address());
            statement.setString(8, application.description());
            statement.setString(9, application.publicId());
            return statement;
        }, keyHolder);
        Long organizationId = KeyExtractUtils.extractId(keyHolder);
        jdbcTemplate.update("""
                INSERT INTO organization_managers (organization_id, user_id, manager_role)
                VALUES (?, ?, 'OWNER')
                """, organizationId, application.applicantUserId());
        return organizationId;
    }

    public Long ensureOrganizationForApprovedApplication(ManagerApplicationResponse application) {
        List<Long> existing = jdbcTemplate.query("""
                SELECT om.organization_id
                FROM organization_managers om
                JOIN organizations o ON o.id = om.organization_id
                WHERE om.user_id = ? AND om.left_at IS NULL
                  AND LOWER(o.name) = LOWER(?) AND o.is_deleted = FALSE
                """, (rs, rowNum) -> rs.getLong(1),
                application.applicantUserId(),
                application.plannedCenterName() == null
                        ? application.organizationName()
                        : application.plannedCenterName()
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String centerName = application.plannedCenterName() == null
                ? application.organizationName()
                : application.plannedCenterName();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO organizations (
                        name, organization_type, registration_number, phone,
                        verification_status, created_at
                    ) VALUES (?, ?, ?, ?, 'VERIFIED', CURRENT_TIMESTAMP)
                    """, new String[] { "id" });
            statement.setString(1, centerName);
            statement.setString(2, application.organizationType());
            statement.setString(3, application.registrationNumber());
            statement.setString(4, application.contact());
            return statement;
        }, keyHolder);
        Long organizationId = KeyExtractUtils.extractId(keyHolder);
        jdbcTemplate.update("""
                INSERT INTO organization_managers (organization_id, user_id, manager_role)
                VALUES (?, ?, 'OWNER')
                """, organizationId, application.applicantUserId());
        return organizationId;
    }

    public boolean managesOrganization(Long userId, Long organizationId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM organization_managers om
                JOIN organizations o ON o.id = om.organization_id
                WHERE om.user_id = ? AND om.organization_id = ?
                  AND om.left_at IS NULL
                  AND o.verification_status = 'VERIFIED'
                  AND o.is_deleted = FALSE
                """, Integer.class, userId, organizationId);
        return count != null && count > 0;
    }

    public List<OrganizationResponse> findManagedOrganizations(Long userId) {
        return jdbcTemplate.query("""
                SELECT o.*
                FROM organizations o
                JOIN organization_managers om ON om.organization_id = o.id
                WHERE om.user_id = ? AND om.left_at IS NULL AND o.is_deleted = FALSE
                ORDER BY o.created_at DESC
                """, this::mapOrganization, userId);
    }

    public Optional<OrganizationResponse> findOrganization(Long organizationId, boolean publicOnly) {
        String sql = """
                SELECT * FROM organizations
                WHERE id = ? AND is_deleted = FALSE
                """ + (publicOnly ? " AND verification_status = 'VERIFIED'" : "");
        return jdbcTemplate.query(sql, this::mapOrganization, organizationId).stream().findFirst();
    }

    public void updateOrganization(Long organizationId, OrganizationUpdateRequest request) {
        jdbcTemplate.update("""
                UPDATE organizations
                SET name = COALESCE(?, name),
                    phone = COALESCE(?, phone),
                    email = COALESCE(?, email),
                    address = COALESCE(?, address),
                    description = COALESCE(?, description),
                    homepage_url = COALESCE(?, homepage_url),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = FALSE
                """,
                blankToNull(request.name()),
                blankToNull(request.phone()),
                blankToNull(request.email()),
                blankToNull(request.address()),
                blankToNull(request.description()),
                blankToNull(request.homepageUrl()),
                organizationId
        );
    }

    public CenterDashboardResponse dashboard(Long organizationId) {
        long published = count("""
                SELECT COUNT(*) FROM opportunities
                WHERE organization_id = ? AND status = 'PUBLISHED' AND is_deleted = FALSE
                """, organizationId);
        long closed = count("""
                SELECT COUNT(*) FROM opportunities
                WHERE organization_id = ? AND status IN ('RECRUITMENT_CLOSED', 'CANCELLED')
                  AND is_deleted = FALSE
                """, organizationId);
        long pending = count("""
                SELECT COUNT(*) FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                WHERE o.organization_id = ? AND a.status IN ('APPLIED', 'IN_REVIEW')
                """, organizationId);
        long participants = count("""
                SELECT COUNT(*) FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                WHERE o.organization_id = ? AND a.status IN ('APPROVED', 'COMPLETED', 'VERIFIED')
                  AND a.updated_at >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                """, organizationId);
        long totalCommitments = count("""
                SELECT COUNT(*) FROM commitments
                WHERE organization_id = ? AND commitment_status <> 'CANCELLED'
                """, organizationId);
        long signedCommitments = count("""
                SELECT COUNT(*) FROM commitments c
                JOIN clm_documents d ON d.commitment_id = c.id AND d.is_deleted = FALSE
                WHERE c.organization_id = ? AND d.status = 'SIGNED'
                """, organizationId);
        long awaitingSignature = count("""
                SELECT COUNT(*) FROM commitments c
                JOIN clm_documents d ON d.commitment_id = c.id AND d.is_deleted = FALSE
                WHERE c.organization_id = ?
                  AND d.status IN ('PENDING_SIGNATURE', 'SIGNING', 'PARTIALLY_SIGNED')
                """, organizationId);
        BigDecimal signedPledgeAmount = sum("""
                SELECT COALESCE(SUM(c.pledge_amount), 0) FROM commitments c
                JOIN clm_documents d ON d.commitment_id = c.id AND d.is_deleted = FALSE
                WHERE c.organization_id = ? AND d.status = 'SIGNED'
                """, organizationId);
        Long renewalDueCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM commitments
                WHERE organization_id = ? AND commitment_status = 'ACTIVE'
                  AND renewal_due_at IS NOT NULL
                  AND renewal_due_at <= ?
                """, Long.class, organizationId, java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(30)));
        long renewalDueSoon = renewalDueCount == null ? 0 : renewalDueCount;
        return new CenterDashboardResponse(
                organizationId, published, closed, pending, participants,
                totalCommitments, signedCommitments, awaitingSignature,
                signedPledgeAmount, renewalDueSoon
        );
    }

    private long count(String sql, Long organizationId) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, organizationId);
        return count == null ? 0 : count;
    }

    private BigDecimal sum(String sql, Long organizationId) {
        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class, organizationId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private List<ManagerApplicationResponse> queryManagerApplications(String suffix, Object... args) {
        String sql = """
                SELECT ma.*, u.email AS applicant_email
                FROM manager_applications ma
                JOIN users u ON u.id = ma.applicant_user_id
                """ + suffix;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ManagerApplicationResponse(
                rs.getString("public_id"),
                rs.getLong("applicant_user_id"),
                rs.getString("applicant_email"),
                rs.getString("organization_name"),
                rs.getString("position"),
                rs.getString("contact"),
                rs.getString("organization_type"),
                rs.getString("business_registration_number"),
                nullableLong(rs.getObject("proof_file_id")),
                rs.getString("reason"),
                rs.getString("planned_center_name"),
                rs.getString("status"),
                rs.getString("rejection_reason"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at"))
        ), args);
    }

    private List<OrganizationApplicationResponse> queryOrganizationApplications(
            String suffix,
            Object... args
    ) {
        String sql = """
                SELECT oa.*, u.email AS applicant_email, o.id AS created_organization_id
                FROM organization_applications oa
                JOIN users u ON u.id = oa.applicant_user_id
                LEFT JOIN organizations o
                  ON o.source_application_id = oa.id AND o.is_deleted = FALSE
                """ + suffix;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrganizationApplicationResponse(
                rs.getString("public_id"),
                rs.getLong("applicant_user_id"),
                rs.getString("applicant_email"),
                rs.getString("name"),
                rs.getString("organization_type"),
                rs.getString("registration_number"),
                rs.getString("representative_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getString("description"),
                nullableLong(rs.getObject("proof_file_id")),
                rs.getString("status"),
                rs.getString("rejection_reason"),
                nullableLong(rs.getObject("created_organization_id")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at"))
        ), args);
    }

    private OrganizationResponse mapOrganization(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new OrganizationResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("organization_type"),
                rs.getString("registration_number"),
                rs.getString("representative_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getString("description"),
                rs.getString("homepage_url"),
                rs.getBoolean("can_issue_donation_receipt"),
                rs.getString("verification_status"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
