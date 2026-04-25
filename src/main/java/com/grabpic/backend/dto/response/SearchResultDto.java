package com.grabpic.backend.dto.response;

import lombok.Data;

@Data
public class SearchResultDto {
    
    private Long assetId;
    private String assetUrl;
    private String thumbnailUrl;
    private Double similarityScore;
    
    public SearchResultDto(Long assetId, String assetUrl, String thumbnailUrl, Double similarityScore) {
        this.assetId = assetId;
        this.assetUrl = assetUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.similarityScore = similarityScore;
    }
}
