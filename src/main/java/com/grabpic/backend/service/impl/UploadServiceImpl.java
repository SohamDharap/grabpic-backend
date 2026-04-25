package com.grabpic.backend.service.impl;

import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.entity.FaceEmbeddings;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.PythonFaceService;
import com.grabpic.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private final AssetRepository assetRepository;
    private final EventRepository eventRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final PythonFaceService pythonFaceService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public Object uploadImage(Long eventId, MultipartFile file, Long photographerId) {
        try {
            // Validate event exists and belongs to photographer
            EventDetails event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

            if (!event.getPhotographerId().equals(photographerId)) {
                throw new RuntimeException("You are not authorized to upload to this event");
            }

            // Check if file has a face
            if (!pythonFaceService.hasFace(file)) {
                return UploadResponseDto.error("No face detected in the image");
            }

            // Store file locally
            String assetUrl = storeFile(file, eventId);
            String thumbnailUrl = assetUrl; // For now, same URL

            // Save asset details
            AssetDetails asset = AssetDetails.builder()
                    .eventId(eventId)
                    .photographerId(photographerId)
                    .assetUrl(assetUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .size(file.getSize())
                    .isDeleted(false)
                    .build();

            AssetDetails savedAsset = assetRepository.save(asset);

            // Extract and save face embedding
            List<Double> embedding = pythonFaceService.extractFaceEmbedding(file);
            float[] embeddingArray = convertDoubleListToFloatArray(embedding);

            FaceEmbeddings faceEmbedding = FaceEmbeddings.builder()
                    .assetId(savedAsset.getId())
                    .embedding(embeddingArray)
                    .build();

            faceEmbeddingRepository.save(faceEmbedding);

            log.info("Successfully uploaded and processed image for event: {}, asset: {}", eventId, savedAsset.getId());

            return UploadResponseDto.success(
                    savedAsset.getId(),
                    assetUrl,
                    thumbnailUrl,
                    file.getSize(),
                    eventId
            );

        } catch (Exception e) {
            log.error("Error uploading image: {}", e.getMessage());
            return UploadResponseDto.error("Failed to upload image: " + e.getMessage());
        }
    }

    private String storeFile(MultipartFile file, Long eventId) throws IOException {
        // Create upload directory if it doesn't exist
        Path eventDir = Paths.get(uploadDir, "event_" + eventId);
        if (!Files.exists(eventDir)) {
            Files.createDirectories(eventDir);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + originalFilename;
        
        Path filePath = eventDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString().replace("\\", "/");
    }

    private float[] convertDoubleListToFloatArray(List<Double> embedding) {
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}
