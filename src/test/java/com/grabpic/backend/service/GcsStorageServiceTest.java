package com.grabpic.backend.service;

import com.google.api.gax.paging.Page;
import com.google.auth.ServiceAccountSigner;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.grabpic.backend.config.properties.GcpProperties;
import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.service.impl.GcsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GcsStorageServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private ServiceAccountSigner serviceAccountSigner;

    private GcpStorageProperties storageProperties;
    private GcpProperties gcpProperties;
    private GcsStorageService gcsStorageService;

    @BeforeEach
    void setUp() {
        storageProperties = new GcpStorageProperties();
        storageProperties.setBucket("grabpic-media");
        storageProperties.setLocation("asia-south1");
        storageProperties.setObjectPrefix("events");
        storageProperties.getSignedUrl().setEnabled(true);
        storageProperties.getSignedUrl().setDurationMinutes(15);
        storageProperties.getSignedUrl().setMaxDurationMinutes(60);
        storageProperties.getSignedUrl().setSigningServiceAccount("test-sa@project.iam.gserviceaccount.com");

        gcpProperties = new GcpProperties();
        gcpProperties.setProjectId("grabpic-test-project");

        gcsStorageService = new GcsStorageService(storageProperties, gcpProperties, storage, serviceAccountSigner);
    }

    @Test
    @DisplayName("Upload stores bytes to GCS with correct bucket and key")
    void testUpload_Success() {
        String objectKey = "events/1/original/photo.jpg";
        byte[] payload = "image-bytes".getBytes();
        String contentType = "image/jpeg";

        String result = gcsStorageService.upload(objectKey, payload, contentType);

        assertEquals(objectKey, result);
        ArgumentCaptor<BlobInfo> blobCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(1)).create(blobCaptor.capture(), eq(payload));

        BlobInfo savedInfo = blobCaptor.getValue();
        assertEquals("grabpic-media", savedInfo.getBucket());
        assertEquals(objectKey, savedInfo.getName());
        assertEquals(contentType, savedInfo.getContentType());
    }

    @Test
    @DisplayName("Upload fails fast on null or blank object key")
    void testUpload_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () ->
                gcsStorageService.upload("", "bytes".getBytes(), "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () ->
                gcsStorageService.upload(null, "bytes".getBytes(), "image/jpeg"));
    }

    @Test
    @DisplayName("Download returns bytes from GCS")
    void testDownload_Success() {
        String objectKey = "events/1/original/photo.jpg";
        byte[] expected = "downloaded-content".getBytes();
        when(storage.readAllBytes("grabpic-media", objectKey)).thenReturn(expected);

        byte[] actual = gcsStorageService.download(objectKey);

        assertArrayEquals(expected, actual);
        verify(storage, times(1)).readAllBytes("grabpic-media", objectKey);
    }

    @Test
    @DisplayName("Download wraps StorageException in RuntimeException")
    void testDownload_StorageException() {
        String objectKey = "events/1/original/missing.jpg";
        when(storage.readAllBytes("grabpic-media", objectKey))
                .thenThrow(new StorageException(404, "Not Found"));

        assertThrows(RuntimeException.class, () -> gcsStorageService.download(objectKey));
    }

    @Test
    @DisplayName("Generate V4 signed URL delegates to Storage.signUrl with IAM signer")
    void testGenerateSignedUrl_Success() throws Exception {
        String objectKey = "events/1/thumbnail/thumb.jpg";
        URL mockUrl = URI.create("https://storage.googleapis.com/grabpic-media/events/1/thumbnail/thumb.jpg?X-Goog-Signature=abc").toURL();

        when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class)))
                .thenReturn(mockUrl);

        String signedUrl = gcsStorageService.generateSignedUrl(objectKey, Duration.ofMinutes(15));

        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("storage.googleapis.com"));
        verify(storage, times(1)).signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class));
    }

    @Test
    @DisplayName("Generate signed URL clamps requested duration to max-duration-minutes")
    void testGenerateSignedUrl_ClampsToMaxDuration() throws Exception {
        String objectKey = "events/1/original/large.jpg";
        URL mockUrl = URI.create("https://storage.googleapis.com/grabpic-media/events/1/original/large.jpg?sig=xyz").toURL();

        // Request 120 minutes, max is 60 minutes
        when(storage.signUrl(any(BlobInfo.class), eq(60L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class)))
                .thenReturn(mockUrl);

        String signedUrl = gcsStorageService.generateSignedUrl(objectKey, Duration.ofMinutes(120));

        assertNotNull(signedUrl);
        verify(storage, times(1)).signUrl(any(BlobInfo.class), eq(60L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class));
    }

    @Test
    @DisplayName("Delete single object calls storage.delete with bucket and key")
    void testDelete_Success() {
        String objectKey = "events/1/original/photo.jpg";
        when(storage.delete("grabpic-media", objectKey)).thenReturn(true);

        gcsStorageService.delete(objectKey);

        verify(storage, times(1)).delete("grabpic-media", objectKey);
    }

    @Test
    @DisplayName("DeletePrefix lists blobs and deletes them in batch")
    void testDeletePrefix_Success() {
        String prefix = "events/1/";
        Blob mockBlob1 = mock(Blob.class);
        Blob mockBlob2 = mock(Blob.class);
        BlobId id1 = BlobId.of("grabpic-media", "events/1/original/1.jpg");
        BlobId id2 = BlobId.of("grabpic-media", "events/1/thumbnail/1.jpg");

        when(mockBlob1.getBlobId()).thenReturn(id1);
        when(mockBlob2.getBlobId()).thenReturn(id2);

        @SuppressWarnings("unchecked")
        Page<Blob> mockPage = mock(Page.class);
        when(mockPage.iterateAll()).thenReturn(List.of(mockBlob1, mockBlob2));
        when(storage.list(eq("grabpic-media"), any(Storage.BlobListOption.class))).thenReturn(mockPage);
        when(storage.delete(anyList())).thenReturn(List.of(true, true));

        gcsStorageService.deletePrefix(prefix);

        verify(storage, times(1)).list(eq("grabpic-media"), any(Storage.BlobListOption.class));
        verify(storage, times(1)).delete(eq(List.of(id1, id2)));
    }

    @Test
    @DisplayName("Exists returns true when blob exists in GCS")
    void testExists_True() {
        String objectKey = "events/1/original/photo.jpg";
        Blob mockBlob = mock(Blob.class);
        when(mockBlob.exists()).thenReturn(true);
        when(storage.get(eq("grabpic-media"), eq(objectKey), any(Storage.BlobGetOption.class)))
                .thenReturn(mockBlob);

        boolean exists = gcsStorageService.exists(objectKey);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists returns false when blob does not exist in GCS")
    void testExists_False() {
        String objectKey = "events/1/original/missing.jpg";
        when(storage.get(eq("grabpic-media"), eq(objectKey), any(Storage.BlobGetOption.class)))
                .thenReturn(null);

        boolean exists = gcsStorageService.exists(objectKey);

        assertFalse(exists);
    }
}
