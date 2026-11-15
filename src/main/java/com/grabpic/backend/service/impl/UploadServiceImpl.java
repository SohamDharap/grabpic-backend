package com.grabpic.backend.service.impl;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.config.properties.UploadProperties;
import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.service.AsyncFaceProcessingService;
import com.grabpic.backend.service.StorageService;
import com.grabpic.backend.service.ThumbnailService;
import com.grabpic.backend.service.UploadService;
import com.grabpic.backend.util.ByteArrayMultipartFile;
import com.grabpic.backend.util.FileValidationUtil;
import com.grabpic.backend.util.ObjectKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private final EventRepository eventRepository;
    private final AssetRepository assetRepository;
    private final AsyncFaceProcessingService asyncFaceProcessingService;
    private final StorageService storageService;
    private final ThumbnailService thumbnailService;

    private final GcpStorageProperties gcpStorageProperties;
    private final UploadProperties uploadProperties;
    private final StorageProperties storageProperties;

    // ── Existing single-file upload (backward compatible) ────────────────────

    @Override
    public Object uploadImage(Long eventId, MultipartFile file, Long photographerId) {
        logUploadDetails(file != null ? List.of(file) : List.of());
        try {
            EventDetails event = validateEventOwnership(eventId, photographerId);
            return processSingleFile(file, event, null, photographerId);
        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            log.warn("No face detected in uploaded image: {}", file != null ? file.getOriginalFilename() : "null");
            throw new RuntimeException("No face detected in the image. Please upload an image with a clearly visible face.");
        } catch (FaceEmbeddingException.InvalidImageException e) {
            log.warn("Invalid image format: {}", file != null ? file.getOriginalFilename() : "null");
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
            log.error("Unexpected error during upload: {}", e.getMessage(), e);
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    // ── Bulk (multi-file) upload ─────────────────────────────────────────────

    @Override
    public BulkUploadResponseDto uploadImages(Long eventId, Long subEventId,
                                              List<MultipartFile> files, Long photographerId) {
        logUploadDetails(files);
        EventDetails event = validateEventOwnership(eventId, photographerId);

        if (subEventId != null) {
            validateSubEventOwnership(subEventId, eventId, photographerId);
        }

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("No files provided for upload.");
        }
        int maxFiles = uploadProperties.getMaxFiles();
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
                if (!FileValidationUtil.isAllowedContentType(file, uploadProperties.getAllowedContentTypes())) {
                    results.add(UploadResponseDto.fileFailure(originalFilename,
                            "Unsupported file type. Only JPEG, PNG, and WebP images are allowed."));
                    continue;
                }
                long maxSizeBytes = uploadProperties.getMaxImageSizeMb() * 1024 * 1024;
                if (file.getSize() > maxSizeBytes) {
                    results.add(UploadResponseDto.fileFailure(originalFilename,
                            "File exceeds maximum allowed image size of " + uploadProperties.getMaxImageSizeMb() + " MB."));
                    continue;
                }

                UploadResponseDto result = processSingleFile(file, event, subEventId, photographerId);
                results.add(result);
            } catch (FaceEmbeddingException.NoFaceDetectedException e) {
                log.warn("No face detected in file: {}", originalFilename);
                results.add(UploadResponseDto.skipped(originalFilename, "No face detected — image skipped."));
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

        if (subEventId != null) {
            validateSubEventOwnership(subEventId, eventId, photographerId);
        }

        if (!FileValidationUtil.isZipFile(zipFile)) {
            throw new RuntimeException("Uploaded file is not a valid ZIP archive.");
        }

        long maxZipBytes = uploadProperties.getMaxZipSizeMb() * 1024 * 1024;
        if (zipFile.getSize() > maxZipBytes) {
            throw new RuntimeException(
                    "ZIP file exceeds the maximum allowed size of " + uploadProperties.getMaxZipSizeMb() + " MB.");
        }

        Path tempDir = null;
        List<UploadResponseDto> results = new ArrayList<>();
        int maxFiles = uploadProperties.getMaxFiles();

        try {
            String customZipDir = uploadProperties.getZipTempDir();
            if (customZipDir != null && !customZipDir.isBlank()) {
                Path customPath = Paths.get(customZipDir);
                Files.createDirectories(customPath);
                tempDir = Files.createTempDirectory(customPath, "grabpic_zip_");
            } else {
                tempDir = Files.createTempDirectory("grabpic_zip_");
            }
            log.info("Extracting ZIP '{}' to ephemeral temp dir: {}", zipFile.getOriginalFilename(), tempDir);

            Path tempZipFile = tempDir.resolve("upload.zip");
            Files.copy(zipFile.getInputStream(), tempZipFile, StandardCopyOption.REPLACE_EXISTING);

            List<Path> extractedImages = new ArrayList<>();

            try (ZipFile zip = new ZipFile(tempZipFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

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

                    String entryName = Paths.get(entry.getName()).getFileName().toString();
                    if (!FileValidationUtil.hasValidImageExtension(entryName)) {
                        log.debug("Skipping non-image ZIP entry: {}", entryName);
                        continue;
                    }

                    if (extractedImages.size() >= maxFiles) {
                        log.warn("ZIP contains more than {} images; excess entries will be skipped.", maxFiles);
                        results.add(UploadResponseDto.skipped(entryName,
                                "Skipped: maximum file count (" + maxFiles + ") reached."));
                        continue;
                    }

                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zip.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedImages.add(entryPath);
                        log.debug("Extracted: {}", entryName);
                    }
                }
            }

            log.info("Extracted {} image(s) from ZIP for event={}", extractedImages.size(), eventId);

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

    // ── Shared core processing with GCS Cloud Storage ────────────────────────

    /**
     * Core processing pipeline for a single image file.
     * Computes SHA-256 hash -> checks duplicate -> uploads original and thumbnail to GCS -> persists asset (status UPLOADED) -> triggers async face processing.
     */
    private UploadResponseDto processSingleFile(MultipartFile file, EventDetails event,
                                                Long subEventId, Long photographerId)
            throws IOException {

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
        byte[] fileBytes = file.getBytes();
        String contentHash = calculateSha256(fileBytes);

        // Deduplication check: check if an active asset with identical hash already exists in this event
        Optional<AssetDetails> existingAssetOpt = assetRepository
                .findFirstByEventIdAndContentHashAndIsDeleted(event.getId(), contentHash, false);

        if (existingAssetOpt.isPresent()) {
            AssetDetails existingAsset = existingAssetOpt.get();
            log.info("Duplicate file detected for eventId={}: hash={}, existingAssetId={}",
                    event.getId(), contentHash, existingAsset.getId());

            String returnAssetUrl = resolveReturnUrl(existingAsset.getAssetUrl());
            String returnThumbnailUrl = resolveReturnUrl(existingAsset.getThumbnailUrl());

            return UploadResponseDto.duplicate(
                    existingAsset.getId(),
                    returnAssetUrl,
                    returnThumbnailUrl,
                    existingAsset.getSize(),
                    existingAsset.getEventId(),
                    existingAsset.getSubEventId(),
                    originalFilename,
                    contentHash,
                    existingAsset.getStatus()
            );
        }

        // Generate GCS object keys using centralized configuration
        String originalKey = ObjectKeyGenerator.generateOriginalKey(
                gcpStorageProperties, event.getId(), contentHash, originalFilename);
        String thumbnailKey = ObjectKeyGenerator.generateThumbnailKey(
                gcpStorageProperties, event.getId(), contentHash, originalFilename);

        String contentType = file.getContentType() != null ? file.getContentType() : resolveContentType(originalFilename);

        // Generate thumbnail bytes using ThumbnailService
        byte[] thumbnailBytes = thumbnailService.generateThumbnail(fileBytes, contentType);

        String uploadedOriginalKey = null;
        String uploadedThumbnailKey = null;

        try {
            // Upload original image to Cloud Storage (GCS)
            uploadedOriginalKey = storageService.upload(originalKey, fileBytes, contentType);

            // Upload thumbnail image to Cloud Storage if generated, otherwise point thumbnail to original
            String persistedThumbnailKey;
            if (thumbnailBytes != null && thumbnailBytes.length > 0) {
                uploadedThumbnailKey = storageService.upload(thumbnailKey, thumbnailBytes, "image/jpeg");
                persistedThumbnailKey = uploadedThumbnailKey;
            } else {
                persistedThumbnailKey = uploadedOriginalKey;
            }

            // Persist Asset Details in PostgreSQL storing GCS Object Keys (never signed URLs)
            AssetDetails asset = AssetDetails.builder()
                    .eventId(event.getId())
                    .subEventId(subEventId)
                    .photographerId(photographerId)
                    .assetUrl(uploadedOriginalKey)
                    .thumbnailUrl(persistedThumbnailKey)
                    .size(file.getSize())
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .contentHash(contentHash)
                    .status("UPLOADED")
                    .isDeleted(false)
                    .build();

            AssetDetails savedAsset = assetRepository.save(asset);

            // Queue asynchronous background face embedding processing passing the GCS object key
            asyncFaceProcessingService.processAssetFaceEmbeddings(savedAsset.getId(), uploadedOriginalKey);

            log.info("Uploaded to GCS and queued face processing: assetId={}, originalKey={}, thumbnailKey={}, eventId={}",
                    savedAsset.getId(), uploadedOriginalKey, persistedThumbnailKey, event.getId());

            // Prepare URLs for API response (signed URLs if configured, otherwise object keys)
            String returnAssetUrl = resolveReturnUrl(uploadedOriginalKey);
            String returnThumbnailUrl = resolveReturnUrl(persistedThumbnailKey);

            return UploadResponseDto.uploaded(
                    savedAsset.getId(), returnAssetUrl, returnThumbnailUrl, file.getSize(),
                    event.getId(), subEventId, originalFilename, contentHash);

        } catch (Exception e) {
            log.error("Failed during storage upload or DB save for '{}': {}", originalFilename, e.getMessage());
            // Compensating transaction: remove uploaded GCS objects if DB save or subsequent step failed
            if (storageProperties.isDeleteOnUploadFailure()) {
                if (uploadedOriginalKey != null) {
                    storageService.delete(uploadedOriginalKey);
                }
                if (uploadedThumbnailKey != null) {
                    storageService.delete(uploadedThumbnailKey);
                }
            }
            throw new RuntimeException("Upload processing failed: " + e.getMessage(), e);
        }
    }

    private String resolveReturnUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (storageProperties.isUseSignedUrlsForUploadResponses()) {
            try {
                Duration duration = Duration.ofMinutes(gcpStorageProperties.getSignedUrl().getDurationMinutes());
                return storageService.generateSignedUrl(objectKey, duration);
            } catch (Exception e) {
                log.warn("Could not generate signed URL for upload response (key={}): {}", objectKey, e.getMessage());
                return objectKey;
            }
        }
        return objectKey;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

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

    private String resolveContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

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

        log.info(sb.toString());
    }
}
