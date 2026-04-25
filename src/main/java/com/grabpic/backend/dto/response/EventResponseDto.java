package com.grabpic.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
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
}
