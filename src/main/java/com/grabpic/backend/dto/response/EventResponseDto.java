package com.grabpic.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EventResponseDto {

    private Long id;
    private String name;
    private String venue;
    private LocalDateTime eventDate;
    private String ownerName;
    private String ownerContact;
    private Long photographerId;
    private UUID publicToken;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Hierarchy fields — null for main events, set for sub-events
    private Long parentEventId;
    private String description;

    // Populated when fetching a parent event; null/empty when fetching a sub-event
    private List<EventResponseDto> subEvents;
}

