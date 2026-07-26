package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.exception.FaceEmbeddingException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FaceEmbeddingService {
    
    /**
     * Extract all detected face embeddings and bounding boxes from an image file.
     * 
     * @param file Image file containing faces
     * @return EmbeddingResponseDto containing metadata for all detected faces
     * @throws FaceEmbeddingException If face detection fails, no face is detected, or service is unavailable
     */
    EmbeddingResponseDto getAllFaces(MultipartFile file) throws FaceEmbeddingException;

    /**
     * Extract primary (first) face embedding from image file.
     * 
     * @param file Image file containing face
     * @return List of 512 float values representing face embedding
     * @throws FaceEmbeddingException If face detection fails or service is unavailable
     */
    List<Double> getEmbedding(MultipartFile file) throws FaceEmbeddingException;
    
    /**
     * Check if image contains at least one detectable face.
     * 
     * @param file Image file to check
     * @return true if at least one face is detected, false otherwise
     * @throws FaceEmbeddingException If service is unavailable
     */
    boolean hasFace(MultipartFile file) throws FaceEmbeddingException;
}
