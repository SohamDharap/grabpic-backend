package com.grabpic.backend.service;

import com.grabpic.backend.dto.request.CreateEventDto;
import com.grabpic.backend.dto.response.EventResponseDto;

import java.util.List;

public interface EventService {
    
    EventResponseDto createEvent(CreateEventDto request, Long photographerId);
    
    List<EventResponseDto> getEventsByPhotographer(Long photographerId);
    
    EventResponseDto getEventByPublicToken(String publicToken);
}
