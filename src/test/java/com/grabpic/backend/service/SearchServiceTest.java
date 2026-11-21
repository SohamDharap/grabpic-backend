package com.grabpic.backend.service;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.dto.response.SearchResultDto;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @Mock
    private FaceEmbeddingService faceEmbeddingService;

    @Mock
    private StorageService storageService;

    private GcpStorageProperties gcpStorageProperties;
    private StorageProperties storageProperties;
    private SearchServiceImpl searchService;

    private final UUID publicToken = UUID.randomUUID();
    private EventDetails event;

    @BeforeEach
    void setUp() {
        gcpStorageProperties = new GcpStorageProperties();
        gcpStorageProperties.getSignedUrl().setEnabled(true);
        gcpStorageProperties.getSignedUrl().setDurationMinutes(15);

        storageProperties = new StorageProperties();
        storageProperties.setGenerateSignedUrlsForThumbnails(true);

        searchService = new SearchServiceImpl(
                eventRepository,
                faceEmbeddingRepository,
                faceEmbeddingService,
                storageService,
                gcpStorageProperties,
                storageProperties
        );

        event = new EventDetails();
        event.setId(1L);
        event.setPublicToken(publicToken);
        event.setIsActive(true);
    }

    @Test
    @DisplayName("searchByFace should return both assetUrl and thumbnailUrl signed URLs")
    void testSearchByFace_Success_ReturnsAssetUrlAndThumbnailUrl() {
        when(eventRepository.findByPublicTokenAndIsActive(eq(publicToken), eq(true)))
                .thenReturn(Optional.of(event));

        MockMultipartFile selfie = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", new byte[]{1, 2, 3});

        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            embedding.add(0.05);
        }
        when(faceEmbeddingService.getEmbedding(any())).thenReturn(embedding);

        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{10L, "events/1/original/photo1.jpg", "events/1/thumbnail/thumb1.jpg", 0.20});
        dbResults.add(new Object[]{20L, "events/1/original/photo2.jpg", "events/1/thumbnail/thumb2.jpg", 0.90}); // > 0.75 distance, excluded

        when(faceEmbeddingRepository.findSimilarEmbeddings(eq(1L), anyString(), eq(20)))
                .thenReturn(dbResults);

        when(storageService.generateSignedUrl(eq("events/1/thumbnail/thumb1.jpg"), any(Duration.class)))
                .thenReturn("https://storage.googleapis.com/thumb1-signed");
        when(storageService.generateSignedUrl(eq("events/1/original/photo1.jpg"), any(Duration.class)))
                .thenReturn("https://storage.googleapis.com/photo1-signed");

        List<SearchResultDto> results = searchService.searchByFace(publicToken.toString(), selfie);

        assertNotNull(results);
        assertEquals(1, results.size());

        SearchResultDto match = results.get(0);
        assertEquals(10L, match.getAssetId());
        assertEquals("https://storage.googleapis.com/photo1-signed", match.getAssetUrl());
        assertEquals("https://storage.googleapis.com/thumb1-signed", match.getThumbnailUrl());
        assertEquals(0.80, match.getSimilarityScore(), 0.001);

        verify(storageService).generateSignedUrl(eq("events/1/original/photo1.jpg"), any(Duration.class));
        verify(storageService).generateSignedUrl(eq("events/1/thumbnail/thumb1.jpg"), any(Duration.class));
    }

    @Test
    @DisplayName("searchByFace should fallback gracefully if storage signing fails")
    void testSearchByFace_StorageSigningFailure_FallsBackToRawKeys() {
        when(eventRepository.findByPublicTokenAndIsActive(eq(publicToken), eq(true)))
                .thenReturn(Optional.of(event));

        MockMultipartFile selfie = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", new byte[]{1, 2, 3});

        List<Double> embedding = List.of(0.1, 0.2);
        when(faceEmbeddingService.getEmbedding(any())).thenReturn(embedding);

        List<Object[]> dbResults = new ArrayList<>();
        dbResults.add(new Object[]{10L, "events/1/original/photo1.jpg", "events/1/thumbnail/thumb1.jpg", 0.10});
        when(faceEmbeddingRepository.findSimilarEmbeddings(eq(1L), anyString(), eq(20)))
                .thenReturn(dbResults);

        when(storageService.generateSignedUrl(anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("GCS sign error"));

        List<SearchResultDto> results = searchService.searchByFace(publicToken.toString(), selfie);

        assertEquals(1, results.size());
        SearchResultDto match = results.get(0);
        assertEquals(10L, match.getAssetId());
        assertEquals("events/1/original/photo1.jpg", match.getAssetUrl());
        assertEquals("events/1/thumbnail/thumb1.jpg", match.getThumbnailUrl());
    }

    @Test
    @DisplayName("searchByFace throws friendly exception when no face detected")
    void testSearchByFace_NoFaceDetected() {
        when(eventRepository.findByPublicTokenAndIsActive(eq(publicToken), eq(true)))
                .thenReturn(Optional.of(event));

        MockMultipartFile selfie = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(faceEmbeddingService.getEmbedding(any()))
                .thenThrow(new FaceEmbeddingException.NoFaceDetectedException("No face found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> searchService.searchByFace(publicToken.toString(), selfie));

        assertTrue(ex.getMessage().contains("No face detected in the selfie"));
    }
}
