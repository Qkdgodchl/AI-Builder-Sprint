package com.pixelcare.domain.ai.dto;

import java.util.List;

public class AiRecommendResponse {

    private String reply;
    private List<RecommendedCardDto> recommendedCards;

    public AiRecommendResponse() {}

    public AiRecommendResponse(String reply, List<RecommendedCardDto> recommendedCards) {
        this.reply = reply;
        this.recommendedCards = recommendedCards;
    }

    public String getReply() { return reply; }
    public List<RecommendedCardDto> getRecommendedCards() { return recommendedCards; }
}
