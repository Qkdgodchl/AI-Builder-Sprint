package com.pixelcare.domain.user.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 온기가 오른 내역.
 *
 * 값 자체는 WarmthService가 JdbcTemplate으로 넣고 읽지만,
 * Flyway를 끄고 JPA ddl-auto로 스키마를 만드는 구성이라
 * 테이블이 생기도록 엔티티로도 선언해 둔다.
 */
@Entity
@Table(
        name = "warmth_events",
        indexes = @Index(name = "idx_warmth_events_user_day", columnList = "user_id, created_at")
)
public class WarmthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal delta;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WarmthEvent() {
    }

    public WarmthEvent(Long userId, String reason, BigDecimal delta) {
        this.userId = userId;
        this.reason = reason;
        this.delta = delta;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getReason() { return reason; }
    public BigDecimal getDelta() { return delta; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
