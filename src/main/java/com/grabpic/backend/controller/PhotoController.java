package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.ApiResponseDto;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.service.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * Get all photos for a main event.
     */
    @GetMapping("/api/events/{eventId}/photos")
    public ResponseEntity<List<UploadResponseDto>> getPhotosByEvent(
            @PathVariable Long eventId,
            @RequestAttribute("userId") Long photographerId) {

        log.info("Fetching all photos for event id={} by photographer={}", eventId, photographerId);
        List<UploadResponseDto> photos = photoService.getPhotosByEvent(eventId, photographerId);
        return ResponseEntity.ok(photos);
    }

    /**
     * Get all photos for a specific sub-event.
     */
    @GetMapping("/api/events/{eventId}/sub-events/{subEventId}/photos")
    public ResponseEntity<List<UploadResponseDto>> getPhotosBySubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestAttribute("userId") Long photographerId) {

        log.info("Fetching photos for sub-event id={} under event id={} by photographer={}",
                subEventId, eventId, photographerId);
        List<UploadResponseDto> photos = photoService.getPhotosBySubEvent(eventId, subEventId, photographerId);
        return ResponseEntity.ok(photos);
    }

    /**
     * Public endpoint to view photos for an event by public UUID token.
     */
    @GetMapping("/api/public/event/{publicToken}/photos")
    public ResponseEntity<List<UploadResponseDto>> getPhotosByPublicToken(
            @PathVariable String publicToken) {

        log.info("Public fetch photos for event public token: {}", publicToken);
        List<UploadResponseDto> photos = photoService.getPhotosByPublicToken(publicToken);
        return ResponseEntity.ok(photos);
    }

    /**
     * Hard delete a single photo asset.
     */
    @DeleteMapping("/api/events/{eventId}/photos/{assetId}")
    public ResponseEntity<ApiResponseDto> deletePhoto(
            @PathVariable Long eventId,
            @PathVariable Long assetId,
            @RequestAttribute("userId") Long photographerId) {

        log.info("Deleting photo asset id={} from event id={} by photographer={}", assetId, eventId, photographerId);
        photoService.deletePhoto(eventId, assetId, photographerId);
        return ResponseEntity.ok(new ApiResponseDto(true, "Photo asset id=" + assetId + " deleted successfully."));
    }
}
