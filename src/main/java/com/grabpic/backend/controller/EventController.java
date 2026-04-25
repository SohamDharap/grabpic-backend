package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;
import com.grabpic.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestBody CreateEventDto request,
            @RequestAttribute("userId") Long photographerId) {
        EventResponseDto response = eventService.createEvent(request, photographerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getEventsByPhotographer(
            @RequestAttribute("userId") Long photographerId) {
        List<EventResponseDto> events = eventService.getEventsByPhotographer(photographerId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/public/{publicToken}")
    public ResponseEntity<EventResponseDto> getEventByPublicToken(
            @PathVariable String publicToken) {
        EventResponseDto event = eventService.getEventByPublicToken(publicToken);
        return ResponseEntity.ok(event);
    }
}
