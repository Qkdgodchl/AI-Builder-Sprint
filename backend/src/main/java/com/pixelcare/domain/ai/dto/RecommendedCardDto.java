package com.pixelcare.domain.ai.dto;

public class RecommendedCardDto {

    private Long opportunityId;
    private String title;
    private String region;
    private String category;
    private String badgeReward;

    public RecommendedCardDto() {}

    public RecommendedCardDto(Long opportunityId, String title, String region, String category, String badgeReward) {
        this.opportunityId = opportunityId;
        this.title = title;
        this.region = region;
        this.category = category;
        this.badgeReward = badgeReward != null ? badgeReward : "LV1_SEED";
    }

    public Long getOpportunityId() { return opportunityId; }
    public String getTitle() { return title; }
    public String getRegion() { return region; }
    public String getCategory() { return category; }
    public String getBadgeReward() { return badgeReward; }
}
