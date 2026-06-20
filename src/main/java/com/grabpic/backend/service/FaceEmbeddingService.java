package com.grabpic.backend.service;

import com.grabpic.backend.exception.FaceEmbeddingException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FaceEmbeddingService {
    
    /**
     * Extract face embedding from image file
     * 
     * @param file Image file containing face
     * @return List of 512 float values representing face embedding
     * @throws FaceEmbeddingException If face detection fails or service is unavailable
     */
    List<Double> getEmbedding(MultipartFile file) throws FaceEmbeddingException;
    
    /**
     * Check if image contains a detectable face
     * 
     * @param file Image file to check
     * @return true if face is detected, false otherwise
     * @throws FaceEmbeddingException If service is unavailable
     */
    boolean hasFace(MultipartFile file) throws FaceEmbeddingException;
}
