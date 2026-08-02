package com.pixelcare.domain.user.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 선행 활동에 따라 회원의 온기를 올린다.
 *
 * 한 번에 크게 오르면 숫자가 의미를 잃으므로 활동별 상승폭을 작게 두고,
 * 하루에 오를 수 있는 총량과 도달 상한을 함께 건다.
 * 오른 내역은 warmth_events에 남겨 하루 상한 계산과 사후 확인에 쓴다.
 */
@Service
public class WarmthService {

    /** 가입 시 기본 온기. */
    public static final BigDecimal BASE = new BigDecimal("36.5");
    /** 온기 수호자 뱃지 목표와 같은 값. 여기서 멈춘다. */
    public static final BigDecimal MAX = new BigDecimal("50.0");
    /** 하루에 오를 수 있는 총량. */
    public static final BigDecimal DAILY_CAP = new BigDecimal("2.0");

    public enum Reason {
        POST_WRITTEN("0.3"),
        COMMENT_WRITTEN("0.1"),
        POST_LIKED("0.1"),
        JOURNAL_WRITTEN("0.2"),
        APPLICATION_SUBMITTED("0.5"),
        COMMITMENT_SIGNED("1.0"),
        COMMITMENT_RENEWED("0.5");

        private final BigDecimal delta;

        Reason(String delta) {
            this.delta = new BigDecimal(delta);
        }

        public BigDecimal delta() {
            return delta;
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public WarmthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 활동 하나에 대한 온기를 올리고 실제로 오른 값을 돌려준다.
     * 하루 상한이나 최고 온도에 걸리면 남은 만큼만 오르고, 여유가 없으면 0이다.
     */
    @Transactional
    public BigDecimal award(Long userId, Reason reason) {
        if (userId == null) return BigDecimal.ZERO;

        BigDecimal current = jdbcTemplate.queryForObject(
                "SELECT temperature FROM users WHERE id = ?", BigDecimal.class, userId);
        if (current == null) return BigDecimal.ZERO;

        BigDecimal earnedToday = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(delta), 0) FROM warmth_events
                WHERE user_id = ? AND created_at >= CURDATE()
                """, BigDecimal.class, userId);
        if (earnedToday == null) earnedToday = BigDecimal.ZERO;

        BigDecimal roomToday = DAILY_CAP.subtract(earnedToday);
        BigDecimal roomTotal = MAX.subtract(current);
        BigDecimal granted = reason.delta().min(roomToday).min(roomTotal);
        if (granted.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        granted = granted.setScale(1, RoundingMode.DOWN);
        if (granted.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        jdbcTemplate.update(
                "INSERT INTO warmth_events (user_id, reason, delta) VALUES (?, ?, ?)",
                userId, reason.name(), granted);
        jdbcTemplate.update(
                "UPDATE users SET temperature = LEAST(?, temperature + ?) WHERE id = ?",
                MAX, granted, userId);
        return granted;
    }

    /** 온기를 올리다 실패해도 원래 하려던 활동까지 막지는 않는다. */
    public BigDecimal awardQuietly(Long userId, Reason reason) {
        try {
            return award(userId, reason);
        } catch (RuntimeException error) {
            System.err.println("온기 적립 실패 (" + reason + "): " + error.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
