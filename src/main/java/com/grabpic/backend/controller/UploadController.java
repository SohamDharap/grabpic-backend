package com.grabpic.backend.controller;

import com.grabpic.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/events/{eventId}/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final UploadService uploadService;

    @PostMapping
    public ResponseEntity<Object> uploadImage(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long photographerId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }

        if (!isValidImageFile(file)) {
            return ResponseEntity.badRequest().body("Invalid file type. Only image files are allowed");
        }

        Object result = uploadService.uploadImage(eventId, file, photographerId);
        return ResponseEntity.ok(result);
    }

    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/jpg")
        );
    }
}
