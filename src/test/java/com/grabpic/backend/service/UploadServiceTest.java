package com.grabpic.backend.service;

import com.grabpic.backend.dto.response.BulkUploadResponseDto;
import com.grabpic.backend.dto.response.EmbeddingResponseDto;
import com.grabpic.backend.dto.response.FaceEmbeddingDto;
import com.grabpic.backend.dto.response.UploadResponseDto;
import com.grabpic.backend.entity.AssetDetails;
import com.grabpic.backend.entity.EventDetails;
import com.grabpic.backend.exception.FaceEmbeddingException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @InjectMocks
    private UploadServiceImpl uploadService;

    private AsyncFaceProcessingServiceImpl asyncFaceProcessingServiceReal;

    private EventDetails testEvent;
    private final Long eventId = 1L;
    private final Long photographerId = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadService, "uploadDir", "target/test-uploads");
        ReflectionTestUtils.setField(uploadService, "maxFiles", 50);

        testEvent = EventDetails.builder()
                .id(eventId)
                .photographerId(photographerId)
                .isActive(true)
                .build();

        asyncFaceProcessingServiceReal = new AsyncFaceProcessingServiceImpl(
                assetRepository, faceEmbeddingRepository, faceEmbeddingService);
    }

    @Test
    @DisplayName("Bulk upload saves files, sets status UPLOADED, and triggers async processing")
    void testUploadImages_Success() throws Exception {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(assetRepository.findFirstByEventIdAndContentHashAndIsDeleted(eq(eventId), anyString(), eq(false)))
                .thenReturn(Optional.empty());

        AssetDetails mockSavedAsset = AssetDetails.builder()
                .id(501L)
                .eventId(eventId)
                .photographerId(photographerId)
                .status("UPLOADED")
                .originalFilename("photo.jpg")
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

        verify(asyncFaceProcessingService, times(1))
                .processAssetFaceEmbeddings(eq(501L), any(Path.class));
    }

    @Test
    @DisplayName("Duplicate image upload returns existing asset status and skips re-saving")
    void testUploadImages_DuplicateFile() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        AssetDetails existingAsset = AssetDetails.builder()
                .id(99L)
                .eventId(eventId)
                .photographerId(photographerId)
                .assetUrl("uploads/event_1/existing.jpg")
                .thumbnailUrl("uploads/event_1/existing.jpg")
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

        verify(assetRepository, never()).save(any());
        verify(asyncFaceProcessingService, never()).processAssetFaceEmbeddings(anyLong(), any());
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
    @DisplayName("Async face processing updates status to COMPLETED when faces are detected")
    void testAsyncFaceProcessing_Success() throws Exception {
        Path tempFile = Files.createTempFile("test_img_", ".jpg");
        Files.write(tempFile, "sample-image-bytes".getBytes());

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

        asyncFaceProcessingServiceReal.processAssetFaceEmbeddings(10L, tempFile);

        ArgumentCaptor<AssetDetails> assetCaptor = ArgumentCaptor.forClass(AssetDetails.class);
        verify(assetRepository, atLeastOnce()).save(assetCaptor.capture());

        AssetDetails finalAsset = assetCaptor.getValue();
        assertEquals("COMPLETED", finalAsset.getStatus());
        assertNull(finalAsset.getFailureReason());

        verify(faceEmbeddingRepository, times(1)).insertFaceEmbedding(eq(10L), anyString());

        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Async processing completes non-face photos (venue/decorations) with COMPLETED status and 0 embeddings")
    void testAsyncFaceProcessing_VenuePhotoWithoutFace() throws Exception {
        Path tempFile = Files.createTempFile("test_venue_", ".jpg");
        Files.write(tempFile, "venue-stage-bytes".getBytes());

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

        asyncFaceProcessingServiceReal.processAssetFaceEmbeddings(20L, tempFile);

        ArgumentCaptor<AssetDetails> assetCaptor = ArgumentCaptor.forClass(AssetDetails.class);
        verify(assetRepository, atLeastOnce()).save(assetCaptor.capture());

        AssetDetails finalAsset = assetCaptor.getValue();
        assertEquals("COMPLETED", finalAsset.getStatus());
        assertNull(finalAsset.getFailureReason());

        // 0 face embeddings inserted
        verify(faceEmbeddingRepository, never()).insertFaceEmbedding(anyLong(), anyString());

        Files.deleteIfExists(tempFile);
    }
}

