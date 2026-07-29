package com.pixelcare.domain.ai.dto;

import com.pixelcare.domain.ai.entity.ChatMessage;
import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private String sender;
    private String message;
    private String recommendedMissionsJson;
    private LocalDateTime createdAt;

    public ChatMessageResponse() {}

    public ChatMessageResponse(ChatMessage chatMessage) {
        this.id = chatMessage.getId();
        this.sender = chatMessage.getSender();
        this.message = chatMessage.getMessage();
        this.recommendedMissionsJson = chatMessage.getRecommendedMissionsJson();
        this.createdAt = chatMessage.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getRecommendedMissionsJson() { return recommendedMissionsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
