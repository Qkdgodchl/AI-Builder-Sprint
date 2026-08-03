package com.pixelcare.domain.clm.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClmDocumentAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClmDocumentAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean managerCanAccess(Long managerId, Long documentId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM clm_documents cd
                JOIN opportunities o ON o.id = cd.volunteer_id
                JOIN organization_managers om ON om.organization_id = o.organization_id
                WHERE cd.id = ?
                  AND om.user_id = ?
                  AND om.left_at IS NULL
                  AND cd.is_deleted = FALSE
                """, Integer.class, documentId, managerId);
        return count != null && count > 0;
    }

    /** 센터 대시보드의 약정 현황 카드에서 조직 전체 전자서명 문서를 한 번에 본다. */
    public List<Long> findAccessibleDocumentIdsByOrganization(Long managerId, Long organizationId) {
        return jdbcTemplate.queryForList("""
                SELECT cd.id
                FROM clm_documents cd
                JOIN opportunities o ON o.id = cd.volunteer_id
                JOIN organization_managers om ON om.organization_id = o.organization_id
                WHERE o.organization_id = ?
                  AND om.user_id = ?
                  AND om.left_at IS NULL
                  AND cd.is_deleted = FALSE
                ORDER BY cd.id DESC
                """, Long.class, organizationId, managerId);
    }

    public List<Long> findAccessibleDocumentIds(Long managerId, String applicationPublicId) {
        return jdbcTemplate.queryForList("""
                SELECT cd.id
                FROM clm_documents cd
                JOIN commitments c ON c.id = cd.commitment_id
                JOIN applications a ON a.id = c.application_id
                JOIN opportunities o ON o.id = a.opportunity_id
                JOIN organization_managers om ON om.organization_id = o.organization_id
                WHERE a.public_id = ?
                  AND om.user_id = ?
                  AND om.left_at IS NULL
                  AND cd.is_deleted = FALSE
                ORDER BY cd.id DESC
                """, Long.class, applicationPublicId, managerId);
    }
}
