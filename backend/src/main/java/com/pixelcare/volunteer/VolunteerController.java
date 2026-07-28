package com.pixelcare.volunteer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {

    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @GetMapping
    public ResponseEntity<List<VolunteerResponseDto>> getVolunteers(
            @RequestParam(required = false) String category
    ) {
        List<VolunteerResponseDto> volunteers = volunteerService.getAllVolunteers(category);
        return ResponseEntity.ok(volunteers);
    }

    @PostMapping
    public ResponseEntity<VolunteerResponseDto> createVolunteer(
            @Valid @RequestBody VolunteerRequestDto requestDto
    ) {
        VolunteerResponseDto responseDto = volunteerService.createVolunteer(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}
