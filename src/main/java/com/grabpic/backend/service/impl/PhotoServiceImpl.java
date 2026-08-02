package com.grabpic.backend.service.impl;

import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    @Transactional
    public void deletePhoto(Long eventId, Long assetId, Long photographerId) {
        validateEventOwnership(eventId, photographerId);

        AssetDetails asset = assetRepository.findByIdAndEventId(assetId, eventId)
                .orElseThrow(() -> new RuntimeException("Photo not found with id: " + assetId + " under event id: " + eventId));

        // 1. Delete face embeddings from DB
        faceEmbeddingRepository.deleteByAssetId(asset.getId());

        // 2. Delete physical image file & thumbnail from disk
        deletePhysicalFile(asset.getAssetUrl());
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().equals(asset.getAssetUrl())) {
            deletePhysicalFile(asset.getThumbnailUrl());
        }

        // 3. Hard delete asset record from DB
        assetRepository.delete(asset);
        log.info("Hard deleted photo asset id={} from event id={}", assetId, eventId);
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
        return UploadResponseDto.success(
                asset.getId(),
                asset.getAssetUrl(),
                asset.getThumbnailUrl(),
                asset.getSize(),
                asset.getEventId(),
                asset.getSubEventId(),
                asset.getOriginalFilename()
        );
    }

    private void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            Path path = Paths.get(fileUrl);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Physically deleted file from disk: {}", fileUrl);
            }
        } catch (IOException e) {
            log.warn("Failed to delete physical file {}: {}", fileUrl, e.getMessage());
        }
    }
}
