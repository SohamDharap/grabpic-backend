package com.grabpic.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventDto {
    
    @NotBlank(message = "Event name is required")
    @Size(max = 100, message = "Event name must be less than 100 characters")
    private String name;
    
    @Size(max = 200, message = "Venue must be less than 200 characters")
    private String venue;
    
    private LocalDateTime eventDate;
    
    @Size(max = 100, message = "Owner name must be less than 100 characters")
    private String ownerName;
    
    @Size(max = 20, message = "Owner contact must be less than 20 characters")
    private String ownerContact;
}
