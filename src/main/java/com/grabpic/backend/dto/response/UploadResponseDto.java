package com.grabpic.backend.dto.response;

import lombok.Data;

@Data
public class UploadResponseDto {
    
    private Long assetId;
    private String assetUrl;
    private String thumbnailUrl;
    private Long size;
    private Long eventId;
    private String message;
    
    public static UploadResponseDto success(Long assetId, String assetUrl, String thumbnailUrl, Long size, Long eventId) {
        UploadResponseDto response = new UploadResponseDto();
        response.setAssetId(assetId);
        response.setAssetUrl(assetUrl);
        response.setThumbnailUrl(thumbnailUrl);
        response.setSize(size);
        response.setEventId(eventId);
        response.setMessage("Image uploaded and processed successfully");
        return response;
    }
    
    public static UploadResponseDto error(String message) {
        UploadResponseDto response = new UploadResponseDto();
        response.setMessage(message);
        return response;
    }
}
