package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.UploadResponseDto;

import java.util.List;

public interface PhotoService {

    /**
     * Retrieves all photos associated with a main event with signed URLs for thumbnails.
     */
    List<UploadResponseDto> getPhotosByEvent(Long eventId, Long photographerId);

    /**
     * Retrieves all photos associated with a specific sub-event with signed URLs for thumbnails.
     */
    List<UploadResponseDto> getPhotosBySubEvent(Long eventId, Long subEventId, Long photographerId);

    /**
     * Retrieves all photos of an event for public guests using public token with signed URLs for thumbnails.
     */
    List<UploadResponseDto> getPhotosByPublicToken(String publicToken);

    /**
     * Generates an on-demand signed URL for downloading/viewing the full-resolution original image for public guests.
     */
    String getOriginalPhotoUrlByPublicToken(String publicToken, Long assetId);

    /**
     * Generates an on-demand signed URL for downloading/viewing the full-resolution original image for authenticated photographers.
     */
    String getOriginalPhotoUrlByPhotographer(Long eventId, Long assetId, Long photographerId);

    /**
     * Hard deletes a single photo: removes face embeddings, GCS objects, and DB record.
     */
    void deletePhoto(Long eventId, Long assetId, Long photographerId);
}
