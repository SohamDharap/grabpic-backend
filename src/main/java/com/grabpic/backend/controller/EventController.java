package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.request.CreateSubEventDto;
import com.grabpic.backend.dto.request.UpdateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;
import com.grabpic.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@Slf4j
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ── Main event endpoints ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestBody CreateEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Creating event for photographer: {}", photographerId);
        EventResponseDto response = eventService.createEvent(request, photographerId);
        log.info("Event created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getEventsByPhotographer(
            @RequestAttribute("userId") Long photographerId) {
        log.info("Fetching events for photographer: {}", photographerId);
        List<EventResponseDto> events = eventService.getEventsByPhotographer(photographerId);
        log.info("Found {} events for photographer: {}", events.size(), photographerId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/public/{publicToken}")
    public ResponseEntity<EventResponseDto> getEventByPublicToken(
            @PathVariable String publicToken) {
        log.info("Fetching event by public token: {}", publicToken);
        EventResponseDto event = eventService.getEventByPublicToken(publicToken);
        log.info("Event found: {}", event.getName());
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> getEventById(
            @PathVariable Long eventId,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Fetching event id={} for photographer={}", eventId, photographerId);
        EventResponseDto event = eventService.getEventById(eventId, photographerId);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Updating event id={} for photographer={}", eventId, photographerId);
        EventResponseDto response = eventService.updateEvent(eventId, request, photographerId);
        log.info("Event updated successfully: id={}", eventId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Deleting event id={} for photographer={}", eventId, photographerId);
        eventService.deleteEvent(eventId, photographerId);
        log.info("Event soft-deleted: id={}", eventId);
        return ResponseEntity.noContent().build();
    }

    // ── Sub-event endpoints ──────────────────────────────────────────────────

    @PostMapping("/{eventId}/sub-events")
    public ResponseEntity<EventResponseDto> createSubEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateSubEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Creating sub-event under event id={} for photographer={}", eventId, photographerId);
        EventResponseDto response = eventService.createSubEvent(eventId, request, photographerId);
        log.info("Sub-event created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/sub-events")
    public ResponseEntity<List<EventResponseDto>> getSubEvents(
            @PathVariable Long eventId,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Fetching sub-events for event id={} photographer={}", eventId, photographerId);
        List<EventResponseDto> subEvents = eventService.getSubEvents(eventId, photographerId);
        log.info("Found {} sub-events for event id={}", subEvents.size(), eventId);
        return ResponseEntity.ok(subEvents);
    }

    @GetMapping("/{eventId}/sub-events/{subEventId}")
    public ResponseEntity<EventResponseDto> getSubEventById(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Fetching sub-event id={} under event id={}", subEventId, eventId);
        EventResponseDto subEvent = eventService.getSubEventById(eventId, subEventId, photographerId);
        return ResponseEntity.ok(subEvent);
    }

    @PutMapping("/{eventId}/sub-events/{subEventId}")
    public ResponseEntity<EventResponseDto> updateSubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @Valid @RequestBody UpdateEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Updating sub-event id={} under event id={}", subEventId, eventId);
        EventResponseDto response = eventService.updateSubEvent(eventId, subEventId, request, photographerId);
        log.info("Sub-event updated successfully: id={}", subEventId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}/sub-events/{subEventId}")
    public ResponseEntity<Void> deleteSubEvent(
            @PathVariable Long eventId,
            @PathVariable Long subEventId,
            @RequestAttribute("userId") Long photographerId) {
        log.info("Deleting sub-event id={} under event id={}", subEventId, eventId);
        eventService.deleteSubEvent(eventId, subEventId, photographerId);
        log.info("Sub-event soft-deleted: id={}", subEventId);
        return ResponseEntity.noContent().build();
    }
}

