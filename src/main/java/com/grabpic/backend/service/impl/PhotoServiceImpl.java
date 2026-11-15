package com.grabpic.backend.service.impl;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.PhotoService;
import com.grabpic.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoServiceImpl implements PhotoService {

    private final AssetRepository assetRepository;
    private final EventRepository eventRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final StorageService storageService;
    private final GcpStorageProperties gcpStorageProperties;
    private final StorageProperties storageProperties;

    @Override
    public List<UploadResponseDto> getPhotosByEvent(Long eventId, Long photographerId) {
        validateEventOwnership(eventId, photographerId);
        List<AssetDetails> assets = assetRepository.findByEventId(eventId);
        return assets.stream()
                .map(this::mapToUploadResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UploadResponseDto> getPhotosBySubEvent(Long eventId, Long subEventId, Long photographerId) {
        validateEventOwnership(eventId, photographerId);
        validateSubEventBelongsToEvent(subEventId, eventId, photographerId);
        List<AssetDetails> assets = assetRepository.findByEventIdAndSubEventId(eventId, subEventId);
        return assets.stream()
                .map(this::mapToUploadResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UploadResponseDto> getPhotosByPublicToken(String publicToken) {
        UUID tokenUuid = UUID.fromString(publicToken);
        EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                .orElseThrow(() -> new RuntimeException("Event not found or inactive for token: " + publicToken));

        List<AssetDetails> assets = assetRepository.findByEventId(event.getId());
        return assets.stream()
                .map(this::mapToUploadResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public String getOriginalPhotoUrlByPublicToken(String publicToken, Long assetId) {
        UUID tokenUuid = UUID.fromString(publicToken);
        EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                .orElseThrow(() -> new RuntimeException("Event not found or inactive for token: " + publicToken));

        AssetDetails asset = assetRepository.findByIdAndEventId(assetId, event.getId())
                .orElseThrow(() -> new RuntimeException("Photo not found with id: " + assetId + " under this event"));

        if (Boolean.TRUE.equals(asset.getIsDeleted())) {
            throw new RuntimeException("Photo has been deleted: id=" + assetId);
        }

        Duration duration = Duration.ofMinutes(gcpStorageProperties.getSignedUrl().getDurationMinutes());
        return storageService.generateSignedUrl(asset.getAssetUrl(), duration);
    }

    @Override
    public String getOriginalPhotoUrlByPhotographer(Long eventId, Long assetId, Long photographerId) {
        validateEventOwnership(eventId, photographerId);

        AssetDetails asset = assetRepository.findByIdAndEventId(assetId, eventId)
                .orElseThrow(() -> new RuntimeException("Photo not found with id: " + assetId + " under event id: " + eventId));

        if (Boolean.TRUE.equals(asset.getIsDeleted())) {
            throw new RuntimeException("Photo has been deleted: id=" + assetId);
        }

        Duration duration = Duration.ofMinutes(gcpStorageProperties.getSignedUrl().getDurationMinutes());
        return storageService.generateSignedUrl(asset.getAssetUrl(), duration);
    }

    @Override
    @Transactional
    public void deletePhoto(Long eventId, Long assetId, Long photographerId) {
        validateEventOwnership(eventId, photographerId);

        AssetDetails asset = assetRepository.findByIdAndEventId(assetId, eventId)
                .orElseThrow(() -> new RuntimeException("Photo not found with id: " + assetId + " under event id: " + eventId));

        // 1. Delete face embeddings from DB
        faceEmbeddingRepository.deleteByAssetId(asset.getId());

        // 2. Delete original object and thumbnail object from Cloud Storage (GCS)
        deleteStorageObjectQuietly(asset.getAssetUrl());
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().equals(asset.getAssetUrl())) {
            deleteStorageObjectQuietly(asset.getThumbnailUrl());
        }

        // 3. Hard delete asset record from DB
        assetRepository.delete(asset);
        log.info("Deleted photo asset id={} from event id={}", assetId, eventId);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private EventDetails validateEventOwnership(Long eventId, Long photographerId) {
        EventDetails event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        if (!event.getPhotographerId().equals(photographerId)) {
            throw new RuntimeException("Access denied: event does not belong to this photographer");
        }
        return event;
    }

    private void validateSubEventBelongsToEvent(Long subEventId, Long eventId, Long photographerId) {
        EventDetails subEvent = eventRepository.findById(subEventId)
                .orElseThrow(() -> new RuntimeException("Sub-event not found with id: " + subEventId));
        if (!subEvent.getPhotographerId().equals(photographerId)) {
            throw new RuntimeException("Access denied: sub-event does not belong to this photographer");
        }
        if (!eventId.equals(subEvent.getParentEventId())) {
            throw new RuntimeException("Sub-event id=" + subEventId + " does not belong to event id=" + eventId);
        }
    }

    private UploadResponseDto mapToUploadResponseDto(AssetDetails asset) {
        Duration duration = Duration.ofMinutes(gcpStorageProperties.getSignedUrl().getDurationMinutes());

        String displayThumbnailUrl = asset.getThumbnailUrl();
        if (storageProperties.isGenerateSignedUrlsForThumbnails() && displayThumbnailUrl != null) {
            try {
                displayThumbnailUrl = storageService.generateSignedUrl(displayThumbnailUrl, duration);
            } catch (Exception e) {
                log.warn("Failed to generate signed URL for thumbnail key='{}': {}", asset.getThumbnailUrl(), e.getMessage());
            }
        }

        String displayAssetUrl = null;
        if (!storageProperties.isGenerateOriginalSignedUrlOnDemand() && asset.getAssetUrl() != null) {
            try {
                displayAssetUrl = storageService.generateSignedUrl(asset.getAssetUrl(), duration);
            } catch (Exception e) {
                log.warn("Failed to generate signed URL for asset key='{}': {}", asset.getAssetUrl(), e.getMessage());
            }
        }

        UploadResponseDto dto = UploadResponseDto.success(
                asset.getId(),
                displayAssetUrl,
                displayThumbnailUrl,
                asset.getSize(),
                asset.getEventId(),
                asset.getSubEventId(),
                asset.getOriginalFilename()
        );
        dto.setStatus(asset.getStatus());
        dto.setContentHash(asset.getContentHash());
        dto.setFailureReason(asset.getFailureReason());
        return dto;
    }

    private void deleteStorageObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            storageService.delete(objectKey);
            log.info("Deleted object from storage: {}", objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete storage object '{}': {}", objectKey, e.getMessage());
        }
    }
}
