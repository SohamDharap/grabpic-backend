package com.grabpic.backend.service;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.config.properties.UploadProperties;
import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.dto.response.FaceEmbeddingDto;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.repository.AssetRepository;
import com.grabpic.backend.repository.EventRepository;
import com.grabpic.backend.repository.FaceEmbeddingRepository;
import com.grabpic.backend.service.impl.AsyncFaceProcessingServiceImpl;
import com.grabpic.backend.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @Mock
    private FaceEmbeddingService faceEmbeddingService;

    @Mock
    private AsyncFaceProcessingService asyncFaceProcessingService;

    @Mock
    private StorageService storageService;

    @Mock
    private ThumbnailService thumbnailService;

    private GcpStorageProperties gcpStorageProperties;
    private UploadProperties uploadProperties;
    private StorageProperties storageProperties;

    private UploadServiceImpl uploadService;
    private AsyncFaceProcessingServiceImpl asyncFaceProcessingServiceReal;

    private EventDetails testEvent;
    private final Long eventId = 1L;
    private final Long photographerId = 100L;

    @BeforeEach
    void setUp() {
        gcpStorageProperties = new GcpStorageProperties();
        gcpStorageProperties.setBucket("test-bucket");
        gcpStorageProperties.setObjectPrefix("events");
        gcpStorageProperties.setOriginalDirectory("original");
        gcpStorageProperties.setThumbnailDirectory("thumbnail");

        uploadProperties = new UploadProperties();
        uploadProperties.setMaxFiles(50);
        uploadProperties.setMaxZipSizeMb(500);
        uploadProperties.setMaxImageSizeMb(25);

        storageProperties = new StorageProperties();
        storageProperties.setUseSignedUrlsForUploadResponses(false);
        storageProperties.setDeleteOnUploadFailure(true);

        uploadService = new UploadServiceImpl(
                eventRepository,
                assetRepository,
                asyncFaceProcessingService,
                storageService,
                thumbnailService,
                gcpStorageProperties,
                uploadProperties,
                storageProperties
        );

        asyncFaceProcessingServiceReal = new AsyncFaceProcessingServiceImpl(
                assetRepository,
                faceEmbeddingRepository,
                faceEmbeddingService,
                storageService
        );

        testEvent = EventDetails.builder()
                .id(eventId)
                .photographerId(photographerId)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Bulk upload uploads original and thumbnail to GCS, sets status UPLOADED, and triggers async processing with objectKey")
    void testUploadImages_Success() throws Exception {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(assetRepository.findFirstByEventIdAndContentHashAndIsDeleted(eq(eventId), anyString(), eq(false)))
                .thenReturn(Optional.empty());

        when(storageService.upload(anyString(), any(byte[].class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(thumbnailService.generateThumbnail(any(byte[].class), anyString()))
                .thenReturn("fake-thumb-bytes".getBytes());

        AssetDetails mockSavedAsset = AssetDetails.builder()
                .id(501L)
                .eventId(eventId)
                .photographerId(photographerId)
                .status("UPLOADED")
                .originalFilename("photo.jpg")
                .assetUrl("events/1/original/12345_hash_photo.jpg")
                .thumbnailUrl("events/1/thumbnail/12345_hash_photo.jpg")
                .build();

        when(assetRepository.save(any(AssetDetails.class))).thenReturn(mockSavedAsset);

        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        BulkUploadResponseDto response = uploadService.uploadImages(eventId, null, List.of(file), photographerId);

        assertNotNull(response);
        assertEquals(1, response.getTotalFiles());
        assertEquals(1, response.getSuccessCount());

        UploadResponseDto result = response.getResults().get(0);
        assertEquals("UPLOADED", result.getStatus());
        assertEquals(501L, result.getAssetId());

        // Verify storageService was called for original and thumbnail
        verify(storageService, times(2)).upload(anyString(), any(byte[].class), anyString());

        // Verify async processing was triggered with objectKey string
        verify(asyncFaceProcessingService, times(1))
                .processAssetFaceEmbeddings(eq(501L), anyString());
    }

    @Test
    @DisplayName("Duplicate image upload returns existing asset status and skips storage upload")
    void testUploadImages_DuplicateFile() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        AssetDetails existingAsset = AssetDetails.builder()
                .id(99L)
                .eventId(eventId)
                .photographerId(photographerId)
                .assetUrl("events/1/original/existing.jpg")
                .thumbnailUrl("events/1/thumbnail/existing.jpg")
                .size(100L)
                .status("COMPLETED")
                .build();

        when(assetRepository.findFirstByEventIdAndContentHashAndIsDeleted(eq(eventId), anyString(), eq(false)))
                .thenReturn(Optional.of(existingAsset));

        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "duplicate-content".getBytes());

        BulkUploadResponseDto response = uploadService.uploadImages(eventId, null, List.of(file), photographerId);

        assertNotNull(response);
        assertEquals(1, response.getTotalFiles());

        UploadResponseDto result = response.getResults().get(0);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(99L, result.getAssetId());
        assertTrue(result.getMessage().contains("Duplicate file detected"));

        verify(storageService, never()).upload(anyString(), any(), anyString());
        verify(assetRepository, never()).save(any());
        verify(asyncFaceProcessingService, never()).processAssetFaceEmbeddings(anyLong(), anyString());
    }

    @Test
    @DisplayName("Upload fails if photographer is unauthorized")
    void testUploadImages_UnauthorizedPhotographer() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "content".getBytes());

        assertThrows(RuntimeException.class, () ->
                uploadService.uploadImages(eventId, null, List.of(file), 999L));
    }

    @Test
    @DisplayName("Async face processing downloads from GCS and updates status to COMPLETED when faces are detected")
    void testAsyncFaceProcessing_Success() throws Exception {
        String objectKey = "events/1/original/test.jpg";
        when(storageService.download(objectKey)).thenReturn("sample-image-bytes".getBytes());

        AssetDetails asset = AssetDetails.builder()
                .id(10L)
                .eventId(eventId)
                .originalFilename("test.jpg")
                .contentType("image/jpeg")
                .status("UPLOADED")
                .build();

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));

        FaceEmbeddingDto faceDto = FaceEmbeddingDto.builder()
                .embedding(List.of(0.1, 0.2, 0.3))
                .build();
        EmbeddingResponseDto responseDto = EmbeddingResponseDto.builder()
                .faces(List.of(faceDto))
                .build();

        when(faceEmbeddingService.getAllFaces(any(MultipartFile.class))).thenReturn(responseDto);

        asyncFaceProcessingServiceReal.processAssetFaceEmbeddings(10L, objectKey);

        ArgumentCaptor<AssetDetails> assetCaptor = ArgumentCaptor.forClass(AssetDetails.class);
        verify(assetRepository, atLeastOnce()).save(assetCaptor.capture());

        AssetDetails finalAsset = assetCaptor.getValue();
        assertEquals("COMPLETED", finalAsset.getStatus());
        assertNull(finalAsset.getFailureReason());

        verify(faceEmbeddingRepository, times(1)).insertFaceEmbedding(eq(10L), anyString());
        verify(storageService, times(1)).download(objectKey);
    }

    @Test
    @DisplayName("Async processing completes non-face photos (venue/decorations) with COMPLETED status and 0 embeddings")
    void testAsyncFaceProcessing_VenuePhotoWithoutFace() throws Exception {
        String objectKey = "events/1/original/stage_decorations.jpg";
        when(storageService.download(objectKey)).thenReturn("venue-stage-bytes".getBytes());

        AssetDetails asset = AssetDetails.builder()
                .id(20L)
                .eventId(eventId)
                .originalFilename("stage_decorations.jpg")
                .status("UPLOADED")
                .build();

        when(assetRepository.findById(20L)).thenReturn(Optional.of(asset));

        // Return 0 faces detected (decoration/scenery photo)
        EmbeddingResponseDto emptyFacesResponse = EmbeddingResponseDto.builder()
                .faces(List.of())
                .build();
        when(faceEmbeddingService.getAllFaces(any(MultipartFile.class))).thenReturn(emptyFacesResponse);

        asyncFaceProcessingServiceReal.processAssetFaceEmbeddings(20L, objectKey);

        ArgumentCaptor<AssetDetails> assetCaptor = ArgumentCaptor.forClass(AssetDetails.class);
        verify(assetRepository, atLeastOnce()).save(assetCaptor.capture());

        AssetDetails finalAsset = assetCaptor.getValue();
        assertEquals("COMPLETED", finalAsset.getStatus());
        assertNull(finalAsset.getFailureReason());

        // 0 face embeddings inserted
        verify(faceEmbeddingRepository, never()).insertFaceEmbedding(anyLong(), anyString());
        verify(storageService, times(1)).download(objectKey);
    }
}
