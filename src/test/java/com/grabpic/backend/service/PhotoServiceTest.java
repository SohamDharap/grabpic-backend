package com.grabpic.backend.service;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.impl.PhotoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @Mock
    private StorageService storageService;

    private GcpStorageProperties gcpStorageProperties;
    private StorageProperties storageProperties;
    private PhotoServiceImpl photoService;

    private final Long eventId = 1L;
    private final Long photographerId = 100L;
    private final UUID publicToken = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        gcpStorageProperties = new GcpStorageProperties();
        gcpStorageProperties.getSignedUrl().setEnabled(true);
        gcpStorageProperties.getSignedUrl().setDurationMinutes(15);

        storageProperties = new StorageProperties();
        storageProperties.setGenerateSignedUrlsForThumbnails(true);
        storageProperties.setGenerateOriginalSignedUrlOnDemand(true);

        photoService = new PhotoServiceImpl(
                assetRepository,
                eventRepository,
                faceEmbeddingRepository,
                storageService,
                gcpStorageProperties,
                storageProperties
        );
    }

    @Test
    @DisplayName("getPhotosByEvent returns signed URL for thumbnail and keeps original URL on-demand")
    void testGetPhotosByEvent_SignedThumbnails() {
        EventDetails event = EventDetails.builder().id(eventId).photographerId(photographerId).isActive(true).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        AssetDetails asset = AssetDetails.builder()
                .id(10L)
                .eventId(eventId)
                .assetUrl("events/1/original/photo.jpg")
                .thumbnailUrl("events/1/thumbnail/thumb.jpg")
                .status("COMPLETED")
                .build();
        when(assetRepository.findByEventId(eventId)).thenReturn(List.of(asset));

        when(storageService.generateSignedUrl(eq("events/1/thumbnail/thumb.jpg"), any(Duration.class)))
                .thenReturn("https://storage.googleapis.com/grabpic-media/events/1/thumbnail/thumb.jpg?sig=123");

        List<UploadResponseDto> results = photoService.getPhotosByEvent(eventId, photographerId);

        assertEquals(1, results.size());
        UploadResponseDto dto = results.get(0);
        assertTrue(dto.getThumbnailUrl().contains("https://storage.googleapis.com"));
        assertNull(dto.getAssetUrl()); // on demand
    }

    @Test
    @DisplayName("getOriginalPhotoUrlByPublicToken validates token and returns signed URL for original image")
    void testGetOriginalPhotoUrlByPublicToken_Success() {
        EventDetails event = EventDetails.builder()
                .id(eventId)
                .publicToken(publicToken)
                .isActive(true)
                .build();
        when(eventRepository.findByPublicTokenAndIsActive(publicToken, true)).thenReturn(Optional.of(event));

        AssetDetails asset = AssetDetails.builder()
                .id(15L)
                .eventId(eventId)
                .assetUrl("events/1/original/photo.jpg")
                .isDeleted(false)
                .build();
        when(assetRepository.findByIdAndEventId(15L, eventId)).thenReturn(Optional.of(asset));

        when(storageService.generateSignedUrl(eq("events/1/original/photo.jpg"), any(Duration.class)))
                .thenReturn("https://storage.googleapis.com/grabpic-media/events/1/original/photo.jpg?signed=true");

        String url = photoService.getOriginalPhotoUrlByPublicToken(publicToken.toString(), 15L);

        assertNotNull(url);
        assertTrue(url.contains("signed=true"));
    }

    @Test
    @DisplayName("deletePhoto deletes original and thumbnail from GCS and database")
    void testDeletePhoto_DeletesFromStorageAndDb() {
        EventDetails event = EventDetails.builder().id(eventId).photographerId(photographerId).isActive(true).build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        AssetDetails asset = AssetDetails.builder()
                .id(25L)
                .eventId(eventId)
                .assetUrl("events/1/original/photo.jpg")
                .thumbnailUrl("events/1/thumbnail/thumb.jpg")
                .build();
        when(assetRepository.findByIdAndEventId(25L, eventId)).thenReturn(Optional.of(asset));

        photoService.deletePhoto(eventId, 25L, photographerId);

        verify(faceEmbeddingRepository, times(1)).deleteByAssetId(25L);
        verify(storageService, times(1)).delete("events/1/original/photo.jpg");
        verify(storageService, times(1)).delete("events/1/thumbnail/thumb.jpg");
        verify(assetRepository, times(1)).delete(asset);
    }
}
