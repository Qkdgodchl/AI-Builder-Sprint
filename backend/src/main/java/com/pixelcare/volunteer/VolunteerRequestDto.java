package com.pixelcare.volunteer;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class VolunteerRequestDto {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    private String title;

    @NotBlank(message = "위치는 필수 입력 항목입니다.")
    private String location;

    @NotBlank(message = "주관 기관은 필수 입력 항목입니다.")
    private String organizer;

    private String category;

    private Long targetAmount;

    private Long currentAmount;

    private List<String> tags;

    public VolunteerRequestDto() {}

    public VolunteerRequestDto(String title, String location, String organizer, String category,
                               Long targetAmount, Long currentAmount, List<String> tags) {
        this.title = title;
        this.location = location;
        this.organizer = organizer;
        this.category = category;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.tags = tags;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Long targetAmount) { this.targetAmount = targetAmount; }

    public Long getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

}
