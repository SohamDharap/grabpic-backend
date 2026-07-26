package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.CreatePhotographerDto;
import com.grabpic.backend.dto.response.PhotographerResponseDto;
import com.grabpic.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/photographers")
    public ResponseEntity<PhotographerResponseDto> createPhotographer(
            @Valid @RequestBody CreatePhotographerDto request) {
        log.info("Creating photographer");
        PhotographerResponseDto response = adminService.createPhotographer(request);
        log.info("Photographer created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/activate")
    public ResponseEntity<PhotographerResponseDto> activatePhotographer(
            @PathVariable Long photographerId) {
        log.info("Activating photographer: {}", photographerId);
        PhotographerResponseDto response = adminService.activatePhotographer(photographerId);
        log.info("Photographer activated: {}", photographerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/deactivate")
    public ResponseEntity<PhotographerResponseDto> deactivatePhotographer(
            @PathVariable Long photographerId) {
        log.info("Deactivating photographer: {}", photographerId);
        PhotographerResponseDto response = adminService.deactivatePhotographer(photographerId);
        log.info("Photographer deactivated: {}", photographerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photographers")
    public ResponseEntity<List<PhotographerResponseDto>> getAllPhotographers() {
        log.info("Fetching all photographers");
        List<PhotographerResponseDto> photographers = adminService.getAllPhotographers();
        log.info("Found {} photographers", photographers.size());
        return ResponseEntity.ok(photographers);
    }
}
