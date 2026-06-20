package com.grabpic.backend.service.impl;

import com.grabpic.backend.converter.VectorConverter;
import com.grabpic.backend.dto.response.SearchResultDto;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.FaceEmbeddingService;
import com.grabpic.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

            // Use native pgvector similarity search
            List<Object[]> results = faceEmbeddingRepository.findSimilarEmbeddings(
                    event.getId(), queryVectorString, 20);

            // Convert results to SearchResultDto
            List<SearchResultDto> searchResults = new ArrayList<>();
            for (Object[] result : results) {
                Long assetId = (Long) result[0];
                String assetUrl = (String) result[1];
                String thumbnailUrl = (String) result[2];
                Double distance = (Double) result[3];
                
                // Convert distance to similarity (lower distance = higher similarity)
                double similarity = 1 - Math.min(distance, 2.0); // Normalize to 0-1 range
                
                // Only include matches with similarity above threshold (e.g., 0.3 distance threshold)
                if (distance <= 0.7) {
                    searchResults.add(new SearchResultDto(assetId, assetUrl, thumbnailUrl, similarity));
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
            log.error("Error searching by face: {}", e.getMessage());
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
