package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.SearchResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SearchService {
    
    List<SearchResultDto> searchByFace(String publicToken, MultipartFile selfieFile);
}
