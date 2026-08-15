package com.grabpic.backend.service.impl;

import com.grabpic.backend.converter.VectorConverter;
import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.dto.response.FaceEmbeddingDto;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.FaceEmbeddingService;
import com.grabpic.backend.service.UploadService;
import com.grabpic.backend.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.grabpic.backend.util.ByteArrayMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private final EventRepository eventRepository;
    private final AssetRepository assetRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final FaceEmbeddingService faceEmbeddingService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-files:100}")
    private int maxFiles;

    @Value("${app.upload.max-zip-size-mb:500}")
    private long maxZipSizeMb;

    // ── Existing single-file upload (backward compatible) ────────────────────

    @Override
    public Object uploadImage(Long eventId, MultipartFile file, Long photographerId) {
        logUploadDetails(file != null ? List.of(file) : List.of());
        try {
            EventDetails event = validateEventOwnership(eventId, photographerId);
            return processSingleFile(file, event, null, photographerId);
        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            log.warn("No face detected in uploaded image: {}", file.getOriginalFilename());
            throw new RuntimeException("No face detected in the image. Please upload an image with a clearly visible face.");
        } catch (FaceEmbeddingException.InvalidImageException e) {
            log.warn("Invalid image format: {}", file.getOriginalFilename());
            throw new RuntimeException("Invalid image file. Please upload a valid image (JPEG, PNG, WebP).");
        } catch (FaceEmbeddingException.ServiceUnavailableException e) {
            log.error("Face embedding service unavailable: {}", e.getMessage());
            throw new RuntimeException("Face detection service is temporarily unavailable. Please try again later.");
        } catch (FaceEmbeddingException e) {
            log.error("Error processing face embedding: {}", e.getMessage());
            throw new RuntimeException("Failed to process face embedding: " + e.getMessage());
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during upload: {}", e.getMessage());
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    // ── Bulk (multi-file) upload ─────────────────────────────────────────────

    @Override
    public BulkUploadResponseDto uploadImages(Long eventId, Long subEventId,
                                              List<MultipartFile> files, Long photographerId) {
        logUploadDetails(files);
        EventDetails event = validateEventOwnership(eventId, photographerId);

        // When uploading to a sub-event, validate it exists and belongs to this event
        if (subEventId != null) {
            validateSubEventOwnership(subEventId, eventId, photographerId);
        }

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("No files provided for upload.");
        }
        if (files.size() > maxFiles) {
            throw new RuntimeException("Too many files. Maximum allowed per request is " + maxFiles + ".");
        }

        List<UploadResponseDto> results = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            try {
                if (file.isEmpty()) {
                    results.add(UploadResponseDto.fileFailure(originalFilename, "File is empty."));
                    continue;
                }
                if (!FileValidationUtil.isValidImageFile(file)) {
                    results.add(UploadResponseDto.fileFailure(originalFilename,
                            "Unsupported file type. Only JPEG, PNG, and WebP images are allowed."));
                    continue;
                }
                UploadResponseDto result = processSingleFile(file, event, subEventId, photographerId);
                results.add(result);
            } catch (FaceEmbeddingException.NoFaceDetectedException e) {
                log.warn("No face detected in file: {}", originalFilename);
                results.add(UploadResponseDto.skipped(originalFilename,
                        "No face detected — image skipped."));
            } catch (FaceEmbeddingException.ServiceUnavailableException e) {
                log.error("Face service unavailable while processing: {}", originalFilename);
                results.add(UploadResponseDto.fileFailure(originalFilename,
                        "Face detection service unavailable. Please try again later."));
            } catch (Exception e) {
                log.error("Failed to process file {}: {}", originalFilename, e.getMessage());
                results.add(UploadResponseDto.fileFailure(originalFilename, e.getMessage()));
            }
        }

        BulkUploadResponseDto response = BulkUploadResponseDto.of(results);
        log.info("Bulk upload complete for event={} subEvent={}: total={} success={} failure={} skipped={}",
                eventId, subEventId, response.getTotalFiles(),
                response.getSuccessCount(), response.getFailureCount(), response.getSkippedCount());
        return response;
    }

    // ── ZIP upload ───────────────────────────────────────────────────────────

    @Override
    public BulkUploadResponseDto uploadZip(Long eventId, Long subEventId,
                                           MultipartFile zipFile, Long photographerId) {
        EventDetails event = validateEventOwnership(eventId, photographerId);

        // When uploading to a sub-event, validate it exists and belongs to this event
        if (subEventId != null) {
            validateSubEventOwnership(subEventId, eventId, photographerId);
        }

        // Validate the file is actually a ZIP
        if (!FileValidationUtil.isZipFile(zipFile)) {
            throw new RuntimeException("Uploaded file is not a valid ZIP archive.");
        }

        // Enforce size limit
        long maxZipBytes = maxZipSizeMb * 1024 * 1024;
        if (zipFile.getSize() > maxZipBytes) {
            throw new RuntimeException(
                    "ZIP file exceeds the maximum allowed size of " + maxZipSizeMb + " MB.");
        }

        Path tempDir = null;
        List<UploadResponseDto> results = new ArrayList<>();

        try {
            // Extract ZIP to a temporary directory
            tempDir = Files.createTempDirectory("grabpic_zip_");
            log.info("Extracting ZIP '{}' to temp dir: {}", zipFile.getOriginalFilename(), tempDir);

            // Write ZIP bytes to temp file for ZipFile API
            Path tempZipFile = tempDir.resolve("upload.zip");
            Files.copy(zipFile.getInputStream(), tempZipFile, StandardCopyOption.REPLACE_EXISTING);

            List<Path> extractedImages = new ArrayList<>();

            try (ZipFile zip = new ZipFile(tempZipFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    // Skip directories and hidden/system files
                    if (entry.isDirectory() || entry.getName().startsWith("__MACOSX")) {
                        continue;
                    }

                    // Zip Slip protection
                    Path entryPath = tempDir.resolve(entry.getName());
                    try {
                        FileValidationUtil.validateZipEntryPath(entryPath, tempDir);
                    } catch (IllegalArgumentException zipSlip) {
                        log.warn("Zip Slip attempt detected for entry: {}", entry.getName());
                        results.add(UploadResponseDto.fileFailure(entry.getName(),
                                "Rejected: path traversal detected in ZIP entry."));
                        continue;
                    }

                    // Only process supported image file types by extension
                    String entryName = Paths.get(entry.getName()).getFileName().toString();
                    if (!FileValidationUtil.hasValidImageExtension(entryName)) {
                        log.debug("Skipping non-image ZIP entry: {}", entryName);
                        continue;
                    }

                    // Enforce max-files limit across entries
                    if (extractedImages.size() >= maxFiles) {
                        log.warn("ZIP contains more than {} images; excess entries will be skipped.", maxFiles);
                        results.add(UploadResponseDto.skipped(entryName,
                                "Skipped: maximum file count (" + maxFiles + ") reached."));
                        continue;
                    }

                    // Extract entry
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zip.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedImages.add(entryPath);
                        log.debug("Extracted: {}", entryName);
                    }
                }
            }

            log.info("Extracted {} image(s) from ZIP for event={}", extractedImages.size(), eventId);

            // Process each extracted image file
            for (Path imagePath : extractedImages) {
                String filename = imagePath.getFileName().toString();
                try {
                    byte[] bytes = Files.readAllBytes(imagePath);
                    String contentType = resolveContentType(filename);
                    MultipartFile multipartFile = new ByteArrayMultipartFile(bytes, filename, filename, contentType);

                    UploadResponseDto result = processSingleFile(multipartFile, event, subEventId, photographerId);
                    results.add(result);
                } catch (FaceEmbeddingException.NoFaceDetectedException e) {
                    log.warn("No face detected in ZIP entry: {}", filename);
                    results.add(UploadResponseDto.skipped(filename, "No face detected — image skipped."));
                } catch (FaceEmbeddingException.ServiceUnavailableException e) {
                    log.error("Face service unavailable while processing ZIP entry: {}", filename);
                    results.add(UploadResponseDto.fileFailure(filename,
                            "Face detection service unavailable."));
                } catch (Exception e) {
                    log.error("Failed to process ZIP entry {}: {}", filename, e.getMessage());
                    results.add(UploadResponseDto.fileFailure(filename, e.getMessage()));
                }
            }

        } catch (IOException e) {
            log.error("Error processing ZIP file: {}", e.getMessage());
            throw new RuntimeException("Failed to process ZIP archive: " + e.getMessage());
        } finally {
            // Guaranteed cleanup of temp directory
            if (tempDir != null) {
                deleteDirectoryQuietly(tempDir);
            }
        }

        BulkUploadResponseDto response = BulkUploadResponseDto.of(results);
        log.info("ZIP upload complete for event={} subEvent={}: total={} success={} failure={} skipped={}",
                eventId, subEventId, response.getTotalFiles(),
                response.getSuccessCount(), response.getFailureCount(), response.getSkippedCount());
        return response;
    }

    // ── Shared core processing ────────────────────────────────────────────────

    /**
     * Core processing pipeline for a single image file.
     * Validates → extracts face embeddings → stores file → saves asset + embeddings.
     *
     * @param file           the image file to process
     * @param event          the validated event entity
     * @param subEventId     optional sub-event ID (null = main event)
     * @param photographerId the authenticated photographer
     * @return UploadResponseDto with status SUCCESS
     */
    private UploadResponseDto processSingleFile(MultipartFile file, EventDetails event,
                                                Long subEventId, Long photographerId)
            throws FaceEmbeddingException, IOException {

        String originalFilename = file.getOriginalFilename();

        // Extract face embeddings
        EmbeddingResponseDto embeddingResponse = faceEmbeddingService.getAllFaces(file);
        if (embeddingResponse == null || embeddingResponse.getFaces() == null
                || embeddingResponse.getFaces().isEmpty()) {
            throw new FaceEmbeddingException.NoFaceDetectedException("No face detected in the image.");
        }

        // Store the file
        String assetUrl = storeFile(file, event.getId());
        String thumbnailUrl = assetUrl; // placeholder — same URL until thumbnail generation is implemented

        // Save asset record
        AssetDetails asset = AssetDetails.builder()
                .eventId(event.getId())
                .subEventId(subEventId)
                .photographerId(photographerId)
                .assetUrl(assetUrl)
                .thumbnailUrl(thumbnailUrl)
                .size(file.getSize())
                .originalFilename(originalFilename)
                .contentType(file.getContentType())
                .isDeleted(false)
                .build();

        AssetDetails savedAsset = assetRepository.save(asset);

        // Save all detected face embeddings for this asset
        for (FaceEmbeddingDto face : embeddingResponse.getFaces()) {
            float[] embeddingArray = convertDoubleListToFloatArray(face.getEmbedding());
            String vectorStr = VectorConverter.convertFloatArrayToVectorString(embeddingArray);
            faceEmbeddingRepository.insertFaceEmbedding(savedAsset.getId(), vectorStr);
        }

        log.info("Processed file '{}': assetId={}, faces={}, eventId={}, subEventId={}",
                originalFilename, savedAsset.getId(), embeddingResponse.getFaces().size(),
                event.getId(), subEventId);

        return UploadResponseDto.success(
                savedAsset.getId(), assetUrl, thumbnailUrl, file.getSize(),
                event.getId(), subEventId, originalFilename);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates that an event (main or sub) exists, is active, and is owned by the photographer.
     * For sub-event uploads, the eventId in the path must refer to the main (parent) event.
     */
    private EventDetails validateEventOwnership(Long eventId, Long photographerId) {
        EventDetails event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        if (!event.getPhotographerId().equals(photographerId)) {
            throw new RuntimeException("You are not authorized to upload to this event.");
        }
        if (!event.getIsActive()) {
            throw new RuntimeException("Event is inactive and cannot accept uploads.");
        }
        return event;
    }

    /**
     * Validates that a sub-event exists, is active, belongs to the given parent event,
     * and is owned by the photographer. Called before any upload that targets a sub-event.
     *
     * @param subEventId     the sub-event to validate
     * @param parentEventId  the parent event it must belong to
     * @param photographerId the authenticated photographer
     */
    private void validateSubEventOwnership(Long subEventId, Long parentEventId, Long photographerId) {
        EventDetails subEvent = eventRepository.findById(subEventId)
                .orElseThrow(() -> new RuntimeException(
                        "Sub-event not found with id: " + subEventId));
        if (!subEvent.getIsActive()) {
            throw new RuntimeException(
                    "Sub-event id=" + subEventId + " is inactive and cannot accept uploads.");
        }
        if (!subEvent.getPhotographerId().equals(photographerId)) {
            throw new RuntimeException(
                    "You are not authorized to upload to sub-event id=" + subEventId);
        }
        if (!parentEventId.equals(subEvent.getParentEventId())) {
            throw new RuntimeException(
                    "Sub-event id=" + subEventId + " does not belong to event id=" + parentEventId);
        }
    }

    /**
     * Stores a file under the event's directory and returns the relative file path.
     */
    private String storeFile(MultipartFile file, Long eventId) throws IOException {
        Path eventDir = Paths.get(uploadDir, "event_" + eventId);
        if (!Files.exists(eventDir)) {
            Files.createDirectories(eventDir);
        }

        String originalFilename = file.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + originalFilename;

        Path filePath = eventDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString().replace("\\", "/");
    }

    /**
     * Converts a Double list to a float array for pgvector compatibility.
     */
    private float[] convertDoubleListToFloatArray(List<Double> embedding) {
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    /**
     * Resolves a MIME content type from a filename extension.
     */
    private String resolveContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // default for .jpg / .jpeg
    }

    /**
     * Recursively deletes a directory and all its contents, suppressing any exceptions.
     * Used to clean up temporary ZIP extraction directories.
     */
    private void deleteDirectoryQuietly(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    Files.deleteIfExists(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.debug("Cleaned up temp directory: {}", dir);
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory {}: {}", dir, e.getMessage());
        }
    }

    private void logUploadDetails(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            log.info("Upload request received: files=0");
            return;
        }

        long totalSizeBytes = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Upload request received:%nfiles=%d%n", files.size()));

        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            long bytes = f != null ? f.getSize() : 0;
            totalSizeBytes += bytes;
            double sizeKb = bytes / 1024.0;
            double sizeMb = bytes / (1024.0 * 1024.0);
            sb.append(String.format("file[%d]='%s' contentType='%s' size=%d bytes (%.2f KB, %.2f MB)%n",
                    i, f != null ? f.getOriginalFilename() : "null",
                    f != null ? f.getContentType() : "null",
                    bytes, sizeKb, sizeMb));
        }

        double totalSizeMb = totalSizeBytes / (1024.0 * 1024.0);
        sb.append(String.format("totalSize=%d bytes (%.2f MB)%n", totalSizeBytes, totalSizeMb));
        sb.append("maxSingleFileSize=104857600 bytes (100.00 MB)%n");
        sb.append("maxTotalUploadSize=524288000 bytes (500.00 MB)");

        log.info(sb.toString());
    }
}

