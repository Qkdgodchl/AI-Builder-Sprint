package com.pixelcare.volunteer;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "volunteers")
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category = "VOLUNTEER";

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String organizer;

    private Long targetAmount;

    private Long currentAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "volunteer_tags", joinColumns = @JoinColumn(name = "volunteer_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Volunteer() {}

    public Volunteer(Long id, String title, String category, String location, String organizer,
                     Long targetAmount, Long currentAmount, List<String> tags, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.category = category != null ? category : "VOLUNTEER";
        this.location = location;
        this.organizer = organizer;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.category == null) {
            this.category = "VOLUNTEER";
        }
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static VolunteerBuilder builder() {
        return new VolunteerBuilder();
    }

    public static class VolunteerBuilder {
        private Long id;
        private String title;
        private String category = "VOLUNTEER";
        private String location;
        private String organizer;
        private Long targetAmount;
        private Long currentAmount;
        private List<String> tags = new ArrayList<>();
        private LocalDateTime createdAt;

        public VolunteerBuilder id(Long id) { this.id = id; return this; }
        public VolunteerBuilder title(String title) { this.title = title; return this; }
        public VolunteerBuilder category(String category) { this.category = category; return this; }
        public VolunteerBuilder location(String location) { this.location = location; return this; }
        public VolunteerBuilder organizer(String organizer) { this.organizer = organizer; return this; }
        public VolunteerBuilder targetAmount(Long targetAmount) { this.targetAmount = targetAmount; return this; }
        public VolunteerBuilder currentAmount(Long currentAmount) { this.currentAmount = currentAmount; return this; }
        public VolunteerBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public VolunteerBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Volunteer build() {
            return new Volunteer(id, title, category, location, organizer, targetAmount, currentAmount, tags, createdAt);
        }
    }
}
