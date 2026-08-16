package com.grabpic.backend.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for bulk (multi-file or ZIP) upload operations.
 * Provides per-file granularity with aggregate counts.
 */
@Data
public class BulkUploadResponseDto {

    private int totalFiles;
    private int successCount;
    private int failureCount;
    private int skippedCount;

    /** Per-file results -- each entry contains status, filename, and either asset details or an error message. */
    private List<UploadResponseDto> results;

    public static BulkUploadResponseDto of(List<UploadResponseDto> results) {
        BulkUploadResponseDto dto = new BulkUploadResponseDto();
        dto.setResults(results);
        dto.setTotalFiles(results.size());
        dto.setSuccessCount((int) results.stream().filter(r ->
                "SUCCESS".equals(r.getStatus()) || "UPLOADED".equals(r.getStatus()) || "COMPLETED".equals(r.getStatus())
        ).count());
        dto.setFailureCount((int) results.stream().filter(r -> "FAILURE".equals(r.getStatus())).count());
        dto.setSkippedCount((int) results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count());
        return dto;
    }

}
