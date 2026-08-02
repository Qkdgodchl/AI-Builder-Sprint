package com.pixelcare.domain.connect.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** "나도 이거 원해요" 표시. 같은 요청에 한 사람이 한 번만 남긴다. */
@Entity
@Table(
        name = "connect_supports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_connect_supports_request_user",
                columnNames = {"request_id", "user_id"})
)
public class ConnectSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ConnectSupport() {
    }

    public ConnectSupport(Long requestId, Long userId) {
        this.requestId = requestId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Long getUserId() { return userId; }
}
