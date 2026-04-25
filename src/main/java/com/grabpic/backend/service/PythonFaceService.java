package com.grabpic.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PythonFaceService {
    
    List<Double> extractFaceEmbedding(MultipartFile imageFile);
    
    boolean hasFace(MultipartFile imageFile);
}
