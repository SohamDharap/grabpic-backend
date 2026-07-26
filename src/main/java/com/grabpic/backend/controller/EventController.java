package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.CreateEventDto;
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

    @PostMapping
    log.info("Creating event for photographer: {}", photographerId);
        public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestBody CreateEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        EventResponseDto response = eventService.createEvent(request, photographerId);
        log.info("Event created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    log.info("Fetching events for photographer: {}", photographerId);
        public ResponseEntity<List<EventResponseDto>> getEventsByPhotographer(
            @RequestAttribute("userId") Long photographerId) {
        List<EventResponseDto> events = eventService.getEventsByPhotographer(photographerId);
        log.info("Found {} events for photographer: {}", events.size(), photographerId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/public/{publicToken}")
    log.info("Fetching event by public token: {}", publicToken);
        public ResponseEntity<EventResponseDto> getEventByPublicToken(
            @PathVariable String publicToken) {
        EventResponseDto event = eventService.getEventByPublicToken(publicToken);
        log.info("Event found: {}", event.getName());
        return ResponseEntity.ok(event);
    }
}
