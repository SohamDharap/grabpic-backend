package com.grabpic.backend.service;

import com.grabpic.backend.dto.request.CreatePhotographerDto;
import com.grabpic.backend.dto.response.PhotographerResponseDto;

import java.util.List;

public interface AdminService {
    
    PhotographerResponseDto createPhotographer(CreatePhotographerDto request);
    
    PhotographerResponseDto activatePhotographer(Long photographerId);
    
    PhotographerResponseDto deactivatePhotographer(Long photographerId);
    
    List<PhotographerResponseDto> getAllPhotographers();
}
