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
    log.info("Creating photographer");
        public ResponseEntity<PhotographerResponseDto> createPhotographer(
            @Valid @RequestBody CreatePhotographerDto request) {
        PhotographerResponseDto response = adminService.createPhotographer(request);
        log.info("Photographer created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/activate")
    log.info("Activating photographer: {}", photographerId);
        public ResponseEntity<PhotographerResponseDto> activatePhotographer(
            @PathVariable Long photographerId) {
        PhotographerResponseDto response = adminService.activatePhotographer(photographerId);
        log.info("Photographer activated: {}", photographerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/deactivate")
    log.info("Deactivating photographer: {}", photographerId);
        public ResponseEntity<PhotographerResponseDto> deactivatePhotographer(
            @PathVariable Long photographerId) {
        PhotographerResponseDto response = adminService.deactivatePhotographer(photographerId);
        log.info("Photographer deactivated: {}", photographerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photographers")
    log.info("Fetching all photographers");
        public ResponseEntity<List<PhotographerResponseDto>> getAllPhotographers() {
        List<PhotographerResponseDto> photographers = adminService.getAllPhotographers();
        log.info("Found {} photographers", photographers.size());
        return ResponseEntity.ok(photographers);
    }
}
