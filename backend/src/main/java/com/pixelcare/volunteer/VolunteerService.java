package com.pixelcare.volunteer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;

    public VolunteerService(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    public List<VolunteerResponseDto> getAllVolunteers(String category) {
        List<Volunteer> volunteers;
        if (category != null && !category.isBlank()) {
            volunteers = volunteerRepository.findByCategoryOrderByIdDesc(category.toUpperCase());
        } else {
            volunteers = volunteerRepository.findAllByOrderByIdDesc();
        }

        return volunteers.stream()
                .map(VolunteerResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public VolunteerResponseDto createVolunteer(VolunteerRequestDto requestDto) {
        String category = (requestDto.getCategory() != null && !requestDto.getCategory().isBlank())
                ? requestDto.getCategory().toUpperCase()
                : "VOLUNTEER";

        Volunteer volunteer = Volunteer.builder()
                .title(requestDto.getTitle())
                .category(category)
                .location(requestDto.getLocation())
                .organizer(requestDto.getOrganizer())
                .targetAmount(requestDto.getTargetAmount())
                .currentAmount(requestDto.getCurrentAmount() != null ? requestDto.getCurrentAmount() : 0L)
                .tags(requestDto.getTags() != null ? requestDto.getTags() : List.of())
                .build();

        Volunteer saved = volunteerRepository.save(volunteer);
        return VolunteerResponseDto.fromEntity(saved);
    }
}
