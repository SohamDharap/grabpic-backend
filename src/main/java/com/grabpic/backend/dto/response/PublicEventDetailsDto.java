package com.grabpic.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Publicly exposed event metadata DTO for guest users.
 * Omits sensitive details like ownerContact, photographerId, and publicToken.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicEventDetailsDto {

    private String name;
    private String venue;
    private LocalDateTime eventDate;
    private String description;
    private String ownerName;
}
