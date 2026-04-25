package com.grabpic.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grabpic.backend.service.PythonFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonFaceServiceImpl implements PythonFaceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${python.face.service.url:http://localhost:8001/embed}")
    private String pythonServiceUrl;

    @Override
    public List<Double> extractFaceEmbedding(MultipartFile imageFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonServiceUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
                List<Double> embedding = (List<Double>) responseMap.get("embedding");
                
                if (embedding == null || embedding.isEmpty()) {
                    throw new RuntimeException("No face detected in the image");
                }
                
                log.info("Successfully extracted face embedding with {} dimensions", embedding.size());
                return embedding;
            } else {
                throw new RuntimeException("Python service returned error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error extracting face embedding: {}", e.getMessage());
            throw new RuntimeException("Failed to extract face embedding: " + e.getMessage());
        }
    }

    @Override
    public boolean hasFace(MultipartFile imageFile) {
        try {
            extractFaceEmbedding(imageFile);
            return true;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("No face detected")) {
                return false;
            }
            throw e;
        }
    }
}
