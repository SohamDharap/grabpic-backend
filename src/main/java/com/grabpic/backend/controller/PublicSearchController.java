package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.SearchResultDto;
import com.grabpic.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/public/event/{publicToken}/search")
@RequiredArgsConstructor
@Slf4j
public class PublicSearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<List<SearchResultDto>> searchByFace(
            @PathVariable String publicToken,
            @RequestParam("selfie") MultipartFile selfieFile) {

        if (selfieFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!isValidImageFile(selfieFile)) {
            return ResponseEntity.badRequest().build();
        }

        List<SearchResultDto> results = searchService.searchByFace(publicToken, selfieFile);
        return ResponseEntity.ok(results);
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
