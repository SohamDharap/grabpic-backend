package com.grabpic.backend.dto.response;

import lombok.Data;

@Data
public class UploadResponseDto {

    private Long assetId;
    private String assetUrl;
    private String thumbnailUrl;
    private Long size;
    private Long eventId;
    private Long subEventId;
    private String originalFilename;
    private String contentHash;
    private String failureReason;
    private String message;


    /**
     * Per-file upload status: UPLOADED, PROCESSING, SUCCESS (COMPLETED), FAILURE, or SKIPPED.
     */
    private String status;

    public static UploadResponseDto uploaded(Long assetId, String assetUrl, String thumbnailUrl,
                                             Long size, Long eventId, Long subEventId,
                                             String originalFilename, String contentHash) {
        UploadResponseDto response = new UploadResponseDto();
        response.setAssetId(assetId);
        response.setAssetUrl(assetUrl);
        response.setThumbnailUrl(thumbnailUrl);
        response.setSize(size);
        response.setEventId(eventId);
        response.setSubEventId(subEventId);
        response.setOriginalFilename(originalFilename);
        response.setContentHash(contentHash);
        response.setStatus("UPLOADED");
        response.setMessage("File uploaded successfully. Face processing queued in background.");
        return response;
    }

    public static UploadResponseDto duplicate(Long assetId, String assetUrl, String thumbnailUrl,
                                              Long size, Long eventId, Long subEventId,
                                              String originalFilename, String contentHash, String existingStatus) {
        UploadResponseDto response = new UploadResponseDto();
        response.setAssetId(assetId);
        response.setAssetUrl(assetUrl);
        response.setThumbnailUrl(thumbnailUrl);
        response.setSize(size);
        response.setEventId(eventId);
        response.setSubEventId(subEventId);
        response.setOriginalFilename(originalFilename);
        response.setContentHash(contentHash);
        response.setStatus(existingStatus != null ? existingStatus : "COMPLETED");
        response.setMessage("Duplicate file detected for this event; existing asset reused.");
        return response;
    }

    public static UploadResponseDto success(Long assetId, String assetUrl, String thumbnailUrl,
                                            Long size, Long eventId, Long subEventId,
                                            String originalFilename) {
        UploadResponseDto response = new UploadResponseDto();
        response.setAssetId(assetId);
        response.setAssetUrl(assetUrl);
        response.setThumbnailUrl(thumbnailUrl);
        response.setSize(size);
        response.setEventId(eventId);
        response.setSubEventId(subEventId);
        response.setOriginalFilename(originalFilename);
        response.setStatus("SUCCESS");
        response.setMessage("Image uploaded and processed successfully");
        return response;
    }

    /**
     * Legacy factory kept for backward compatibility with existing single-upload callers.
     */
    public static UploadResponseDto success(Long assetId, String assetUrl, String thumbnailUrl,
                                            Long size, Long eventId) {
        return success(assetId, assetUrl, thumbnailUrl, size, eventId, null, null);
    }

    public static UploadResponseDto error(String message) {
        UploadResponseDto response = new UploadResponseDto();
        response.setStatus("FAILURE");
        response.setMessage(message);
        response.setFailureReason(message);
        return response;
    }

    public static UploadResponseDto fileFailure(String originalFilename, String message) {
        UploadResponseDto response = new UploadResponseDto();
        response.setOriginalFilename(originalFilename);
        response.setStatus("FAILURE");
        response.setMessage(message);
        response.setFailureReason(message);
        return response;
    }

    public static UploadResponseDto skipped(String originalFilename, String message) {
        UploadResponseDto response = new UploadResponseDto();
        response.setOriginalFilename(originalFilename);
        response.setStatus("SKIPPED");
        response.setMessage(message);
        response.setFailureReason(message);
        return response;
    }
}


