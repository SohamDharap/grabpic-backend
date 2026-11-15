package com.grabpic.backend.service.impl;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.converter.VectorConverter;
import com.grabpic.backend.dto.response.SearchResultDto;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.FaceEmbeddingService;
import com.grabpic.backend.service.SearchService;
import com.grabpic.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final EventRepository eventRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final FaceEmbeddingService faceEmbeddingService;
    private final StorageService storageService;
    private final GcpStorageProperties gcpStorageProperties;
    private final StorageProperties storageProperties;

    @Override
    public List<SearchResultDto> searchByFace(String publicToken, MultipartFile selfieFile) {
        try {
            // Validate event exists
            UUID tokenUuid = UUID.fromString(publicToken);
            EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                    .orElseThrow(() -> new RuntimeException("Event not found or inactive"));

            // Extract face embedding from selfie
            List<Double> queryEmbedding = faceEmbeddingService.getEmbedding(selfieFile);

            // Convert embedding to vector string format for pgvector
            float[] queryEmbeddingArray = convertDoubleListToFloatArray(queryEmbedding);
            String queryVectorString = VectorConverter.convertFloatArrayToVectorString(queryEmbeddingArray);

            log.info("Executing pgvector cosine distance search for eventId: {}", event.getId());
            List<Object[]> results = faceEmbeddingRepository.findSimilarEmbeddings(
                    event.getId(), queryVectorString, 20);

            log.info("pgvector returned {} candidate face matches from DB", results.size());

            Duration signedUrlDuration = Duration.ofMinutes(gcpStorageProperties.getSignedUrl().getDurationMinutes());

            // Convert results to SearchResultDto with signed thumbnail URLs
            List<SearchResultDto> searchResults = new ArrayList<>();
            for (Object[] result : results) {
                Long assetId = ((Number) result[0]).longValue();
                String assetKey = (String) result[1];
                String thumbnailKey = (String) result[2];
                double distance = ((Number) result[3]).doubleValue();

                // Cosine similarity = 1 - cosine distance
                double similarity = Math.max(0.0, 1.0 - distance);
                log.info("Candidate assetId: {}, distance: {}, similarity: {}", assetId, distance, similarity);

                // Include matches with Cosine Distance <= 0.75 (Cosine Similarity >= 0.25)
                if (distance <= 0.75) {
                    String signedThumbnailUrl = thumbnailKey;
                    if (storageProperties.isGenerateSignedUrlsForThumbnails() && thumbnailKey != null) {
                        try {
                            signedThumbnailUrl = storageService.generateSignedUrl(thumbnailKey, signedUrlDuration);
                        } catch (Exception e) {
                            log.warn("Failed to sign thumbnail URL for key '{}': {}", thumbnailKey, e.getMessage());
                        }
                    }

                    // For search results, we deliver the thumbnail URL and keep original URL on demand
                    searchResults.add(new SearchResultDto(assetId, null, signedThumbnailUrl, similarity));
                }
            }

            log.info("Found {} matching photos for event {}", searchResults.size(), event.getId());
            return searchResults;

        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            log.warn("No face detected in selfie image");
            throw new RuntimeException("No face detected in the selfie. Please upload an image with a clearly visible face.");
        } catch (FaceEmbeddingException.ServiceUnavailableException e) {
            log.error("Face embedding service unavailable during search: {}", e.getMessage());
            throw new RuntimeException("Face detection service is temporarily unavailable. Please try again later.");
        } catch (FaceEmbeddingException e) {
            log.error("Error processing face embedding for search: {}", e.getMessage());
            throw new RuntimeException("Failed to process face embedding: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error searching by face: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search by face: " + e.getMessage());
        }
    }

    private float[] convertDoubleListToFloatArray(List<Double> embedding) {
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}
