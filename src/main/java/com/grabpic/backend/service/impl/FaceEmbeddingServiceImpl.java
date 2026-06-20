package com.grabpic.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceEmbeddingServiceImpl implements FaceEmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${face.service.url:http://localhost:8001/embed}")
    private String faceServiceUrl;

    @Override
    public List<Double> getEmbedding(MultipartFile file) throws FaceEmbeddingException {
        try {
            log.info("Extracting face embedding from file: {}", file.getOriginalFilename());
            
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Create ByteArrayResource with proper filename
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Make API call to Python service
            ResponseEntity<String> response = restTemplate.postForEntity(
                    faceServiceUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseEmbeddingResponse(response.getBody());
            } else {
                throw new FaceEmbeddingException.ServiceUnavailableException(
                        "Python service returned status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("No face detected in image: {}", file.getOriginalFilename());
                throw new FaceEmbeddingException.NoFaceDetectedException(
                        "No face detected in the image");
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
            log.error("Error reading file: {}", e.getMessage());
            throw new FaceEmbeddingException("Error processing image file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error extracting face embedding: {}", e.getMessage());
            throw new FaceEmbeddingException("Failed to extract face embedding: " + e.getMessage());
        }
    }

    @Override
    public boolean hasFace(MultipartFile file) throws FaceEmbeddingException {
        try {
            getEmbedding(file);
            return true;
        } catch (FaceEmbeddingException.NoFaceDetectedException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Double> parseEmbeddingResponse(String responseBody) throws FaceEmbeddingException {
        try {
            log.debug("Parsing embedding response from Python service");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            if (!responseMap.containsKey("embedding")) {
                throw new FaceEmbeddingException.InvalidResponseException(
                        "Response does not contain 'embedding' field");
            }

            Object embeddingObj = responseMap.get("embedding");
            
            if (!(embeddingObj instanceof List)) {
                throw new FaceEmbeddingException.InvalidResponseException(
                        "Embedding field is not a list");
            }

            @SuppressWarnings("unchecked")
            List<Object> embeddingList = (List<Object>) embeddingObj;
            
            if (embeddingList.isEmpty()) {
                throw new FaceEmbeddingException.NoFaceDetectedException(
                        "Empty embedding returned - no face detected");
            }

            // Convert to List<Double>
            List<Double> embedding = embeddingList.stream()
                    .map(obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).doubleValue();
                        } else {
                            try {
                                return Double.parseDouble(obj.toString());
                            } catch (NumberFormatException e) {
                                throw new FaceEmbeddingException.InvalidResponseException(
                                        "Invalid embedding value: " + obj);
                            }
                        }
                    })
                    .toList();

            if (embedding.size() != 512) {
                log.warn("Embedding size mismatch: expected 512, got {}", embedding.size());
                // Still proceed but log warning
            }

            log.info("Successfully extracted face embedding with {} dimensions", embedding.size());
            return embedding;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
            throw new FaceEmbeddingException.InvalidResponseException(
                    "Invalid JSON response from Python service: " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof FaceEmbeddingException) {
                throw e;
            }
            log.error("Error parsing embedding response: {}", e.getMessage());
            throw new FaceEmbeddingException.InvalidResponseException(
                    "Failed to parse Python service response: " + e.getMessage());
        }
    }
}
