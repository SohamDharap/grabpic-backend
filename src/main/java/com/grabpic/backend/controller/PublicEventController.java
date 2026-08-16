package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.PublicEventDetailsDto;
import com.grabpic.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/event")
@RequiredArgsConstructor
@Slf4j
public class PublicEventController {

    private final EventService eventService;

    /**
     * Public endpoint to fetch basic event details (name, venue, eventDate, description, ownerName) using publicToken.
     * Omits sensitive data like ownerContact, photographerId, and publicToken.
     */
    @GetMapping("/{publicToken}")
    public ResponseEntity<PublicEventDetailsDto> getPublicEventDetails(
            @PathVariable String publicToken) {

        log.info("Public fetch basic event details for token: {}", publicToken);
        PublicEventDetailsDto event = eventService.getPublicEventDetails(publicToken);
        return ResponseEntity.ok(event);
    }
}

