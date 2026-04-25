package com.grabpic.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    
    Object uploadImage(Long eventId, MultipartFile file, Long photographerId);
}
