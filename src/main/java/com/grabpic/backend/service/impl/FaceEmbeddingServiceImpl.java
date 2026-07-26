package com.grabpic.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.dto.response.FaceEmbeddingDto;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.service.FaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceEmbeddingServiceImpl implements FaceEmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${face.service.url:http://localhost:8001/embed}")
    private String faceServiceUrl;

    @Override
    public EmbeddingResponseDto getAllFaces(MultipartFile file) throws FaceEmbeddingException {
        if (file == null || file.isEmpty()) {
            throw new FaceEmbeddingException.InvalidImageException("Uploaded file is empty.");
        }

        try {
            log.info("Extracting face embeddings from file: {}", file.getOriginalFilename());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
                }
            };
            
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    faceServiceUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                EmbeddingResponseDto responseDto = objectMapper.readValue(response.getBody(), EmbeddingResponseDto.class);
                if (responseDto == null || responseDto.getFaces() == null || responseDto.getFaces().isEmpty()) {
                    throw new FaceEmbeddingException.NoFaceDetectedException("No face detected in the image.");
                }
                log.info("Successfully detected {} face(s) in image {}", responseDto.getFaces().size(), file.getOriginalFilename());
                return responseDto;
            } else {
                throw new FaceEmbeddingException.ServiceUnavailableException(
                        "Python face service returned unexpected status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("No face detected in image: {}", file.getOriginalFilename());
                throw new FaceEmbeddingException.NoFaceDetectedException("No face detected in the image");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Invalid/corrupt image file: {}", file.getOriginalFilename());
                throw new FaceEmbeddingException.InvalidImageException("Unable to decode or invalid image file format.");
            } else {
                log.error("Client error from Python service: {}", e.getMessage());
                throw new FaceEmbeddingException.ServiceUnavailableException(
                        "Python service client error: " + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            log.error("Server error from Python service: {}", e.getMessage());
            throw new FaceEmbeddingException.ServiceUnavailableException(
                    "Python service server error: " + e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Python service unavailable at: {}", faceServiceUrl);
            throw new FaceEmbeddingException.ServiceUnavailableException(
                    "Python face service is not available. Please ensure the service is running at: " + faceServiceUrl);
        } catch (IOException e) {
            log.error("Error reading image file bytes: {}", e.getMessage());
            throw new FaceEmbeddingException("Error processing image file: " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof FaceEmbeddingException) {
                throw (FaceEmbeddingException) e;
            }
            log.error("Unexpected error extracting face embeddings: {}", e.getMessage());
            throw new FaceEmbeddingException("Failed to extract face embedding: " + e.getMessage());
        }
    }

    @Override
    public List<Double> getEmbedding(MultipartFile file) throws FaceEmbeddingException {
        EmbeddingResponseDto responseDto = getAllFaces(file);
        List<FaceEmbeddingDto> faces = responseDto.getFaces();
        if (faces == null || faces.isEmpty()) {
            throw new FaceEmbeddingException.NoFaceDetectedException("No face detected in the image");
        }
        return faces.get(0).getEmbedding();
    }

    @Override
    public boolean hasFace(MultipartFile file) throws FaceEmbeddingException {
        try {
            getAllFaces(file);
            return true;
        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            return false;
        }
    }
}
