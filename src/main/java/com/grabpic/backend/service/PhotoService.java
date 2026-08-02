package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.UploadResponseDto;

import java.util.List;

public interface PhotoService {

    /**
     * Retrieves all photos associated with a main event.
     */
    List<UploadResponseDto> getPhotosByEvent(Long eventId, Long photographerId);

    /**
     * Retrieves all photos associated with a specific sub-event.
     */
    List<UploadResponseDto> getPhotosBySubEvent(Long eventId, Long subEventId, Long photographerId);

    /**
     * Retrieves all photos of an event for public guests using public token.
     */
    List<UploadResponseDto> getPhotosByPublicToken(String publicToken);

    /**
     * Hard deletes a single photo: removes face embeddings, physical file from disk, and DB record.
     */
    void deletePhoto(Long eventId, Long assetId, Long photographerId);
}
