package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {

    /** Existing single-file upload — preserved for backward compatibility. */
    Object uploadImage(Long eventId, MultipartFile file, Long photographerId);

    /**
     * Uploads multiple image files in one request.
     * Supports partial success: each file is processed independently.
     *
     * @param eventId        the parent event
     * @param subEventId     optional sub-event (null = main event)
     * @param files          list of image files
     * @param photographerId the authenticated photographer
     * @return bulk result with per-file status
     */
    BulkUploadResponseDto uploadImages(Long eventId, Long subEventId,
                                       List<MultipartFile> files, Long photographerId);

    /**
     * Extracts a ZIP archive and processes each supported image inside it.
     * Applies Zip Slip protection and enforces configured size/count limits.
     *
     * @param eventId        the parent event
     * @param subEventId     optional sub-event (null = main event)
     * @param zipFile        the uploaded ZIP file
     * @param photographerId the authenticated photographer
     * @return bulk result with per-file status
     */
    BulkUploadResponseDto uploadZip(Long eventId, Long subEventId,
                                    MultipartFile zipFile, Long photographerId);
}

