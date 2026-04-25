package com.grabpic.backend.dto.response;

import com.grabpic.backend.enums.GenderType;
import com.grabpic.backend.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotographerResponseDto {
    
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private GenderType gender;
    private Integer age;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
