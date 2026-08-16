package com.grabpic.backend.service.impl;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.request.CreateSubEventDto;
import com.grabpic.backend.dto.request.UpdateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;
import com.grabpic.backend.dto.response.PublicEventDetailsDto;
import com.grabpic.backend.entity.EventDetails;

import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final AssetRepository assetRepository;
    private final com.grabpic.backend.repository.FaceEmbeddingRepository faceEmbeddingRepository;

    // ── Main event operations ────────────────────────────────────────────────

    @Override
    public EventResponseDto createEvent(CreateEventDto request, Long photographerId) {
        EventDetails event = EventDetails.builder()
                .name(request.getName())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .ownerName(request.getOwnerName())
                .ownerContact(request.getOwnerContact())
                .photographerId(photographerId)
                .isActive(true)
                .build();

        EventDetails savedEvent = eventRepository.save(event);
        log.info("Created main event id={} for photographer={}", savedEvent.getId(), photographerId);
        return mapToResponseDto(savedEvent, false);
    }

    @Override
    public List<EventResponseDto> getEventsByPhotographer(Long photographerId) {
        return eventRepository
                .findByPhotographerIdAndIsActiveAndParentEventIdIsNull(photographerId, true)
                .stream()
                .map(e -> mapToResponseDto(e, false))
                .collect(Collectors.toList());
    }

    @Override

    public EventResponseDto getEventByPublicToken(String publicToken) {
        UUID tokenUuid = UUID.fromString(publicToken);
        EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                .orElseThrow(() -> new RuntimeException("Event not found or inactive"));
        return mapToResponseDto(event, false);
    }

    @Override
    public PublicEventDetailsDto getPublicEventDetails(String publicToken) {
        UUID tokenUuid = UUID.fromString(publicToken);
        EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                .orElseThrow(() -> new RuntimeException("Event not found or inactive"));
        return PublicEventDetailsDto.builder()
                .name(event.getName())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .description(event.getDescription())
                .ownerName(event.getOwnerName())
                .build();
    }


    @Override
    public EventResponseDto getEventById(Long eventId, Long photographerId) {
        EventDetails event = findOwnedMainEvent(eventId, photographerId);
        // Include sub-events in response
        List<EventResponseDto> subEvents = eventRepository
                .findByParentEventIdAndPhotographerIdAndIsActive(eventId, photographerId, true)
                .stream()
                .map(e -> mapToResponseDto(e, false))
                .collect(Collectors.toList());
        EventResponseDto dto = mapToResponseDto(event, false);
        dto.setSubEvents(subEvents);
        return dto;
    }

    @Override
    public EventResponseDto updateEvent(Long eventId, UpdateEventDto request, Long photographerId) {
        EventDetails event = findOwnedMainEvent(eventId, photographerId);
        applyUpdates(event, request);
        EventDetails saved = eventRepository.save(event);
        log.info("Updated event id={}", eventId);
        return mapToResponseDto(saved, false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteEvent(Long eventId, Long photographerId) {
        EventDetails event = findOwnedMainEvent(eventId, photographerId);

        // 1. Hard delete all sub-events and their photos/embeddings
        List<EventDetails> subEvents = eventRepository.findByParentEventId(eventId);
        for (EventDetails subEvent : subEvents) {
            List<com.grabpic.backend.entity.AssetDetails> subAssets = assetRepository.findBySubEventId(subEvent.getId());
            for (com.grabpic.backend.entity.AssetDetails asset : subAssets) {
                deleteAssetCompletely(asset);
            }
            eventRepository.delete(subEvent);
            log.info("Hard deleted sub-event id={} under parent event id={}", subEvent.getId(), eventId);
        }

        // 2. Hard delete all main-event level assets
        List<com.grabpic.backend.entity.AssetDetails> mainAssets = assetRepository.findByEventId(eventId);
        for (com.grabpic.backend.entity.AssetDetails asset : mainAssets) {
            deleteAssetCompletely(asset);
        }

        // 3. Hard delete main event record
        eventRepository.delete(event);
        log.info("Hard deleted main event id={} and all associated sub-events & assets", eventId);
    }

    // ── Sub-event operations ─────────────────────────────────────────────────

    @Override
    public EventResponseDto createSubEvent(Long parentEventId, CreateSubEventDto request, Long photographerId) {
        // Validate parent event exists and belongs to photographer
        EventDetails parent = findOwnedMainEvent(parentEventId, photographerId);

        // Circular reference guard: the parent must itself not be a sub-event (max depth = 1)
        if (parent.getParentEventId() != null) {
            throw new RuntimeException(
                    "Cannot create a sub-event of a sub-event. Only one level of nesting is supported.");
        }

        EventDetails subEvent = EventDetails.builder()
                .name(request.getName())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .ownerName(request.getOwnerName())
                .ownerContact(request.getOwnerContact())
                .description(request.getDescription())
                .photographerId(photographerId)
                .parentEventId(parentEventId)
                .isActive(true)
                .build();

        EventDetails saved = eventRepository.save(subEvent);
        log.info("Created sub-event id={} under parent event id={} for photographer={}",
                saved.getId(), parentEventId, photographerId);
        return mapToResponseDto(saved, false);
    }

    @Override
    public List<EventResponseDto> getSubEvents(Long parentEventId, Long photographerId) {
        // Ensure the parent exists and belongs to the photographer
        findOwnedMainEvent(parentEventId, photographerId);
        return eventRepository
                .findByParentEventIdAndPhotographerIdAndIsActive(parentEventId, photographerId, true)
                .stream()
                .map(e -> mapToResponseDto(e, false))
                .collect(Collectors.toList());
    }

    @Override
    public EventResponseDto getSubEventById(Long parentEventId, Long subEventId, Long photographerId) {
        findOwnedMainEvent(parentEventId, photographerId);
        EventDetails subEvent = findOwnedSubEvent(subEventId, parentEventId, photographerId);
        return mapToResponseDto(subEvent, false);
    }

    @Override
    public EventResponseDto updateSubEvent(Long parentEventId, Long subEventId, UpdateEventDto request, Long photographerId) {
        findOwnedMainEvent(parentEventId, photographerId);
        EventDetails subEvent = findOwnedSubEvent(subEventId, parentEventId, photographerId);
        applyUpdates(subEvent, request);
        EventDetails saved = eventRepository.save(subEvent);
        log.info("Updated sub-event id={} under parent event id={}", subEventId, parentEventId);
        return mapToResponseDto(saved, false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteSubEvent(Long parentEventId, Long subEventId, Long photographerId) {
        findOwnedMainEvent(parentEventId, photographerId);
        EventDetails subEvent = findOwnedSubEvent(subEventId, parentEventId, photographerId);

        // Hard delete all assets associated with this sub-event
        List<com.grabpic.backend.entity.AssetDetails> subAssets = assetRepository.findBySubEventId(subEventId);
        for (com.grabpic.backend.entity.AssetDetails asset : subAssets) {
            deleteAssetCompletely(asset);
        }

        // Hard delete sub-event record from DB
        eventRepository.delete(subEvent);
        log.info("Hard deleted sub-event id={} under parent event id={}", subEventId, parentEventId);
    }


    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Finds an active main event (parentEventId IS NULL) owned by the photographer.
     * Throws RuntimeException if not found or not owned.
     */
    private EventDetails findOwnedMainEvent(Long eventId, Long photographerId) {
        return eventRepository.findByIdAndPhotographerIdAndIsActive(eventId, photographerId, true)
                .filter(e -> e.getParentEventId() == null)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    /**
     * Finds an active sub-event owned by the photographer under a specific parent.
     */
    private EventDetails findOwnedSubEvent(Long subEventId, Long parentEventId, Long photographerId) {
        return eventRepository.findByIdAndParentEventIdAndPhotographerIdAndIsActive(
                        subEventId, parentEventId, photographerId, true)
                .orElseThrow(() -> new RuntimeException(
                        "Sub-event not found with id: " + subEventId + " under event: " + parentEventId));
    }

    /**
     * Applies non-null fields from UpdateEventDto to the given EventDetails entity.
     */
    private void applyUpdates(EventDetails event, UpdateEventDto request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            event.setName(request.getName());
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getOwnerName() != null) {
            event.setOwnerName(request.getOwnerName());
        }
        if (request.getOwnerContact() != null) {
            event.setOwnerContact(request.getOwnerContact());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
    }

    private EventResponseDto mapToResponseDto(EventDetails event, boolean includeSubEvents) {
        EventResponseDto response = new EventResponseDto();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setVenue(event.getVenue());
        response.setEventDate(event.getEventDate());
        response.setOwnerName(event.getOwnerName());
        response.setOwnerContact(event.getOwnerContact());
        response.setPhotographerId(event.getPhotographerId());
        response.setPublicToken(event.getPublicToken());
        response.setIsActive(event.getIsActive());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setParentEventId(event.getParentEventId());
        response.setDescription(event.getDescription());
        return response;
    }

    private void deleteAssetCompletely(com.grabpic.backend.entity.AssetDetails asset) {
        if (asset == null) return;
        faceEmbeddingRepository.deleteByAssetId(asset.getId());
        deletePhysicalFile(asset.getAssetUrl());
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().equals(asset.getAssetUrl())) {
            deletePhysicalFile(asset.getThumbnailUrl());
        }
        assetRepository.delete(asset);
        log.info("Hard deleted asset id={}", asset.getId());
    }

    private void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(fileUrl);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.delete(path);
                log.info("Physically deleted file from disk: {}", fileUrl);
            }
        } catch (java.io.IOException e) {
            log.warn("Failed to delete physical file {}: {}", fileUrl, e.getMessage());
        }
    }
}


