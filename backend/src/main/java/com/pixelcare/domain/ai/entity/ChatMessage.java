package com.pixelcare.domain.ai.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String sender; // USER, ASSISTANT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String recommendedMissionsJson;

    public ChatMessage() {}

    public ChatMessage(Long userId, String sender, String message, String recommendedMissionsJson) {
        this.userId = userId;
        this.sender = sender;
        this.message = message;
        this.recommendedMissionsJson = recommendedMissionsJson;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getRecommendedMissionsJson() { return recommendedMissionsJson; }
}
