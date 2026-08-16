package com.grabpic.backend.service;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.request.CreateSubEventDto;
import com.grabpic.backend.dto.request.UpdateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;
import com.grabpic.backend.dto.response.PublicEventDetailsDto;


import java.util.List;


public interface EventService {

    // ── Main event operations ────────────────────────────────────────────────

    EventResponseDto createEvent(CreateEventDto request, Long photographerId);

    List<EventResponseDto> getEventsByPhotographer(Long photographerId);

    EventResponseDto getEventByPublicToken(String publicToken);

    PublicEventDetailsDto getPublicEventDetails(String publicToken);


    EventResponseDto getEventById(Long eventId, Long photographerId);

    EventResponseDto updateEvent(Long eventId, UpdateEventDto request, Long photographerId);

    void deleteEvent(Long eventId, Long photographerId);

    // ── Sub-event operations ─────────────────────────────────────────────────

    EventResponseDto createSubEvent(Long parentEventId, CreateSubEventDto request, Long photographerId);

    List<EventResponseDto> getSubEvents(Long parentEventId, Long photographerId);

    EventResponseDto getSubEventById(Long parentEventId, Long subEventId, Long photographerId);

    EventResponseDto updateSubEvent(Long parentEventId, Long subEventId, UpdateEventDto request, Long photographerId);

    void deleteSubEvent(Long parentEventId, Long subEventId, Long photographerId);
}

