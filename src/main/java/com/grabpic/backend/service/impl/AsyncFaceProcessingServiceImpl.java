package com.grabpic.backend.service.impl;

import com.grabpic.backend.converter.VectorConverter;
import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.dto.response.FaceEmbeddingDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.AsyncFaceProcessingService;
import com.grabpic.backend.service.FaceEmbeddingService;
import com.grabpic.backend.util.ByteArrayMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncFaceProcessingServiceImpl implements AsyncFaceProcessingService {

    private final AssetRepository assetRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final FaceEmbeddingService faceEmbeddingService;

    @Override
    @Async("faceProcessingExecutor")
    public void processAssetFaceEmbeddings(Long assetId, Path filePath) {
        log.info("Starting background face processing for assetId={}, path={}", assetId, filePath);

        AssetDetails asset = assetRepository.findById(assetId).orElse(null);
        if (asset == null || Boolean.TRUE.equals(asset.getIsDeleted())) {
            log.warn("Asset id={} not found or deleted; skipping background face processing.", assetId);
            return;
        }

        // 1. Update state to PROCESSING
        asset.setStatus("PROCESSING");
        asset.setFailureReason(null);
        assetRepository.save(asset);

        try {
            if (!Files.exists(filePath)) {
                throw new IOException("File not found on disk at: " + filePath);
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            String filename = asset.getOriginalFilename() != null ? asset.getOriginalFilename() : "image.jpg";
            String contentType = asset.getContentType() != null ? asset.getContentType() : "image/jpeg";

            MultipartFile multipartFile = new ByteArrayMultipartFile(fileBytes, filename, filename, contentType);

            // 2. Call InsightFace service
            EmbeddingResponseDto responseDto = faceEmbeddingService.getAllFaces(multipartFile);

            if (responseDto == null || responseDto.getFaces() == null || responseDto.getFaces().isEmpty()) {
                throw new FaceEmbeddingException.NoFaceDetectedException("No face detected in the image.");
            }

            // 3. Save face embeddings into pgvector repository
            for (FaceEmbeddingDto face : responseDto.getFaces()) {
                float[] embeddingArray = convertDoubleListToFloatArray(face.getEmbedding());
                String vectorStr = VectorConverter.convertFloatArrayToVectorString(embeddingArray);
                faceEmbeddingRepository.insertFaceEmbedding(asset.getId(), vectorStr);
            }

            // 4. Mark status COMPLETED
            asset.setStatus("COMPLETED");
            asset.setFailureReason(null);
            assetRepository.save(asset);

            log.info("Successfully processed face embeddings for assetId={}: detected {} face(s)",
                    assetId, responseDto.getFaces().size());

        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            log.warn("No face detected for assetId={}: {}", assetId, e.getMessage());
            asset.setStatus("SKIPPED");
            asset.setFailureReason("No face detected — image skipped.");
            assetRepository.save(asset);
        } catch (FaceEmbeddingException.ServiceUnavailableException e) {
            log.error("Face embedding service unavailable for assetId={}: {}", assetId, e.getMessage());
            asset.setStatus("FAILED");
            asset.setFailureReason("Face detection service unavailable.");
            assetRepository.save(asset);
        } catch (Exception e) {
            log.error("Failed face processing for assetId={}: {}", assetId, e.getMessage());
            asset.setStatus("FAILED");
            asset.setFailureReason(e.getMessage());
            assetRepository.save(asset);
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
