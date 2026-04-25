package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.CreatePhotographerDto;
import com.grabpic.backend.dto.response.PhotographerResponseDto;
import com.grabpic.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/photographers")
    public ResponseEntity<PhotographerResponseDto> createPhotographer(
            @Valid @RequestBody CreatePhotographerDto request) {
        PhotographerResponseDto response = adminService.createPhotographer(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/activate")
    public ResponseEntity<PhotographerResponseDto> activatePhotographer(
            @PathVariable Long photographerId) {
        PhotographerResponseDto response = adminService.activatePhotographer(photographerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/photographers/{photographerId}/deactivate")
    public ResponseEntity<PhotographerResponseDto> deactivatePhotographer(
            @PathVariable Long photographerId) {
        PhotographerResponseDto response = adminService.deactivatePhotographer(photographerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photographers")
    public ResponseEntity<List<PhotographerResponseDto>> getAllPhotographers() {
        List<PhotographerResponseDto> photographers = adminService.getAllPhotographers();
        return ResponseEntity.ok(photographers);
    }
}
