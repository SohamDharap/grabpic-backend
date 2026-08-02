package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import com.grabpic.backend.service.UploadService;
import com.grabpic.backend.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    // ── Main event upload endpoints ──────────────────────────────────────────

    /**
     * Single image upload to a main event — original endpoint, fully unchanged.
     */
    @PostMapping("/api/events/{eventId}/upload")
    public ResponseEntity<Object> uploadImage(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long photographerId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }
        if (!FileValidationUtil.isValidImageFile(file)) {
            return ResponseEntity.badRequest().body("Invalid file type. Only image files are allowed");
        }

        log.info("Single upload to event={} by photographer={}", eventId, photographerId);
        Object result = uploadService.uploadImage(eventId, file, photographerId);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk upload of multiple image files to a main event.
     * Supports partial success — each file is processed independently.
     * Max files per request is controlled by {@code app.upload.max-files} (default: 20).
     */
    @PostMapping("/api/events/{eventId}/upload/bulk")
    public ResponseEntity<BulkUploadResponseDto> uploadBulk(
            @PathVariable Long eventId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestAttribute("userId") Long photographerId) {

        log.info("Bulk upload of {} file(s) to event={} by photographer={}", files.size(), eventId, photographerId);
        BulkUploadResponseDto result = uploadService.uploadImages(eventId, null, files, photographerId);
        return ResponseEntity.ok(result);
    }

    /**
     * ZIP archive upload to a main event.
     * Archives are extracted, validated, and each image is processed individually.
     */
    @PostMapping("/api/events/{eventId}/upload/zip")
    public ResponseEntity<BulkUploadResponseDto> uploadZip(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile zipFile,
            @RequestAttribute("userId") Long photographerId) {

        if (zipFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("ZIP upload to event={} by photographer={}, size={}B", eventId, photographerId, zipFile.getSize());
        BulkUploadResponseDto result = uploadService.uploadZip(eventId, null, zipFile, photographerId);
        return ResponseEntity.ok(result);
    }

    // ── Sub-event upload endpoints ────────────────────────────────────────────

    /**
     * Single image upload to a specific sub-event.
     */
    @PostMapping("/api/events/{eventId}/sub-events/{subEventId}/upload")
    public ResponseEntity<Object> uploadImageToSubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long photographerId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }
        if (!FileValidationUtil.isValidImageFile(file)) {
            return ResponseEntity.badRequest().body("Invalid file type. Only image files are allowed");
        }

        log.info("Single upload to event={} sub-event={} by photographer={}", eventId, subEventId, photographerId);
        BulkUploadResponseDto result = uploadService.uploadImages(
                eventId, subEventId, List.of(file), photographerId);

        // Return the single result directly for API consistency
        if (!result.getResults().isEmpty()) {
            return ResponseEntity.ok(result.getResults().get(0));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk upload of multiple image files to a specific sub-event.
     */
    @PostMapping("/api/events/{eventId}/sub-events/{subEventId}/upload/bulk")
    public ResponseEntity<BulkUploadResponseDto> uploadBulkToSubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestAttribute("userId") Long photographerId) {

        log.info("Bulk upload of {} file(s) to event={} sub-event={} by photographer={}",
                files.size(), eventId, subEventId, photographerId);
        BulkUploadResponseDto result = uploadService.uploadImages(eventId, subEventId, files, photographerId);
        return ResponseEntity.ok(result);
    }

    /**
     * ZIP archive upload to a specific sub-event.
     */
    @PostMapping("/api/events/{eventId}/sub-events/{subEventId}/upload/zip")
    public ResponseEntity<BulkUploadResponseDto> uploadZipToSubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestParam("file") MultipartFile zipFile,
            @RequestAttribute("userId") Long photographerId) {

        if (zipFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("ZIP upload to event={} sub-event={} by photographer={}, size={}B",
                eventId, subEventId, photographerId, zipFile.getSize());
        BulkUploadResponseDto result = uploadService.uploadZip(eventId, subEventId, zipFile, photographerId);
        return ResponseEntity.ok(result);
    }
}

