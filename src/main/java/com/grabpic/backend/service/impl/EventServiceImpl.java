package com.grabpic.backend.service.impl;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

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
        return mapToResponseDto(savedEvent);
    }

    @Override
    public List<EventResponseDto> getEventsByPhotographer(Long photographerId) {
        return eventRepository.findByPhotographerIdAndIsActive(photographerId, true).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventResponseDto getEventByPublicToken(String publicToken) {
        UUID tokenUuid = UUID.fromString(publicToken);
        EventDetails event = eventRepository.findByPublicTokenAndIsActive(tokenUuid, true)
                .orElseThrow(() -> new RuntimeException("Event not found or inactive"));
        return mapToResponseDto(event);
    }

    private EventResponseDto mapToResponseDto(EventDetails event) {
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
        return response;
    }
}
