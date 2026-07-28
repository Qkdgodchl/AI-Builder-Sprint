package com.pixelcare.volunteer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final Gov1365ApiService gov1365ApiService;

    public VolunteerService(VolunteerRepository volunteerRepository, Gov1365ApiService gov1365ApiService) {
        this.volunteerRepository = volunteerRepository;
        this.gov1365ApiService = gov1365ApiService;
    }

    public List<VolunteerResponseDto> getAllVolunteers(String category) {
        List<VolunteerResponseDto> result = new ArrayList<>();

        if (category == null || category.isBlank() || "VOLUNTEER".equalsIgnoreCase(category)) {
            List<VolunteerResponseDto> govItems = gov1365ApiService.fetch1365Volunteers();
            result.addAll(govItems);
        }

        List<Volunteer> volunteers;
        if (category != null && !category.isBlank()) {
            volunteers = volunteerRepository.findByCategoryOrderByIdDesc(category.toUpperCase());
        } else {
            volunteers = volunteerRepository.findAllByOrderByIdDesc();
        }

        List<VolunteerResponseDto> dbItems = volunteers.stream()
                .map(VolunteerResponseDto::fromEntity)
                .collect(Collectors.toList());

        result.addAll(dbItems);
        return result;
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
                .link1365(requestDto.getLink1365())
                .build();

        Volunteer saved = volunteerRepository.save(volunteer);
        return VolunteerResponseDto.fromEntity(saved);
    }
}
