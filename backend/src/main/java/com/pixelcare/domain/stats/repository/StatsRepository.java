package com.pixelcare.domain.stats.repository;

import com.pixelcare.domain.stats.dto.PlatformStatsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * 홈 화면 누적 현황을 실제 데이터에서 집계한다.
 * 취소된 약정과 탈퇴 회원은 제외해 화면에 남지 않도록 한다.
 */
@Repository
public class StatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public StatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PlatformStatsResponse summary() {
        return new PlatformStatsResponse(
                averageWarmth(),
                pledgedDonation(),
                volunteerHours(),
                signedCommitments(),
                activeMembers()
        );
    }

    private double averageWarmth() {
        Double average = jdbcTemplate.queryForObject("""
                SELECT AVG(temperature) FROM users
                WHERE is_deleted = FALSE AND account_status = 'ACTIVE'
                """, Double.class);
        return average == null ? 0d : Math.round(average * 10d) / 10d;
    }

    /** 체결되어 유효한 약정의 총 기부 금액. */
    private BigDecimal pledgedDonation() {
        BigDecimal total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(pledge_amount), 0) FROM commitments
                WHERE commitment_status IN ('ACTIVE', 'COMPLETED')
                """, BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * 승인·완료된 봉사 참여의 활동 시간 합계.
     * 봉사 시간을 따로 저장하는 컬럼이 없어 공고의 활동 시작·종료 시각에서 계산한다.
     * 활동 시각이 없는 상시 모집 공고는 집계에서 빠진다.
     */
    private long volunteerHours() {
        return count("""
                SELECT COALESCE(
                           SUM(TIMESTAMPDIFF(MINUTE, o.activity_start_at, o.activity_end_at)) / 60,
                           0)
                FROM applications a
                JOIN opportunities o ON o.id = a.opportunity_id
                WHERE o.opportunity_type = 'VOLUNTEER'
                  AND o.is_deleted = FALSE
                  AND a.status IN ('APPROVED', 'COMPLETED', 'VERIFIED')
                  AND o.activity_start_at IS NOT NULL
                  AND o.activity_end_at IS NOT NULL
                  AND o.activity_end_at > o.activity_start_at
                """);
    }

    /** 모두싸인 전자서명까지 마쳐 증빙이 보관된 약정 건수. */
    private long signedCommitments() {
        return count("""
                SELECT COUNT(*) FROM commitments c
                JOIN clm_documents d ON d.commitment_id = c.id AND d.is_deleted = FALSE
                WHERE d.status = 'SIGNED'
                """);
    }

    private long activeMembers() {
        return count("""
                SELECT COUNT(*) FROM users
                WHERE is_deleted = FALSE AND account_status = 'ACTIVE'
                """);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
