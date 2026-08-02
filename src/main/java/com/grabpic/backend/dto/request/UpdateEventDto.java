package com.grabpic.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing event or sub-event.
 * All fields are optional - only non-null values will be applied.
 */
@Data
public class UpdateEventDto {

    @Size(max = 100, message = "Event name must be less than 100 characters")
    private String name;

    @Size(max = 200, message = "Venue must be less than 200 characters")
    private String venue;

    private LocalDateTime eventDate;

    @Size(max = 100, message = "Owner name must be less than 100 characters")
    private String ownerName;

    @Size(max = 20, message = "Owner contact must be less than 20 characters")
    private String ownerContact;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
}
