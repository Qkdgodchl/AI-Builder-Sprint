package com.pixelcare.volunteer;

import java.util.List;

public class VolunteerResponseDto {

    private Long id;
    private String title;
    private String category;
    private String location;
    private String organizer;
    private Long targetAmount;
    private Long currentAmount;
    private List<String> tags;
    private String link1365;

    public VolunteerResponseDto() {}

    public VolunteerResponseDto(Long id, String title, String category, String location, String organizer,
                                Long targetAmount, Long currentAmount, List<String> tags, String link1365) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.location = location;
        this.organizer = organizer;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.tags = tags;
        this.link1365 = link1365;
    }

    public static VolunteerResponseDto fromEntity(Volunteer volunteer) {
        return new VolunteerResponseDto(
                volunteer.getId(),
                volunteer.getTitle(),
                volunteer.getCategory(),
                volunteer.getLocation(),
                volunteer.getOrganizer(),
                volunteer.getTargetAmount(),
                volunteer.getCurrentAmount(),
                volunteer.getTags(),
                volunteer.getLink1365()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public Long getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Long targetAmount) { this.targetAmount = targetAmount; }

    public Long getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(Long currentAmount) { this.currentAmount = currentAmount; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getLink1365() { return link1365; }
    public void setLink1365(String link1365) { this.link1365 = link1365; }

    public static VolunteerResponseDtoBuilder builder() {
        return new VolunteerResponseDtoBuilder();
    }

    public static class VolunteerResponseDtoBuilder {
        private Long id;
        private String title;
        private String category;
        private String location;
        private String organizer;
        private Long targetAmount;
        private Long currentAmount;
        private List<String> tags;
        private String link1365;

        public VolunteerResponseDtoBuilder id(Long id) { this.id = id; return this; }
        public VolunteerResponseDtoBuilder title(String title) { this.title = title; return this; }
        public VolunteerResponseDtoBuilder category(String category) { this.category = category; return this; }
        public VolunteerResponseDtoBuilder location(String location) { this.location = location; return this; }
        public VolunteerResponseDtoBuilder organizer(String organizer) { this.organizer = organizer; return this; }
        public VolunteerResponseDtoBuilder targetAmount(Long targetAmount) { this.targetAmount = targetAmount; return this; }
        public VolunteerResponseDtoBuilder currentAmount(Long currentAmount) { this.currentAmount = currentAmount; return this; }
        public VolunteerResponseDtoBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public VolunteerResponseDtoBuilder link1365(String link1365) { this.link1365 = link1365; return this; }

        public VolunteerResponseDto build() {
            return new VolunteerResponseDto(id, title, category, location, organizer, targetAmount, currentAmount, tags, link1365);
        }
    }
}
