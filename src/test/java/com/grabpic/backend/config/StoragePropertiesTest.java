package com.grabpic.backend.config;

import com.grabpic.backend.config.properties.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "app.storage.provider=local",
        "app.storage.local.base-dir=target/custom-storage",
        "gcp.project-id=test-project-override",
        "gcp.storage.bucket=override-bucket",
        "gcp.storage.signed-url.duration-minutes=30",
        "app.image.thumbnail.width=500",
        "app.upload.max-files=75",
        "python.face.service.base-url=http://test-face-service:9000"
})
class StoragePropertiesTest {

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private GcpProperties gcpProperties;

    @Autowired
    private GcpStorageProperties gcpStorageProperties;

    @Autowired
    private ImageProcessingProperties imageProcessingProperties;

    @Autowired
    private UploadProperties uploadProperties;

    @Autowired
    private FaceServiceProperties faceServiceProperties;

    @Test
    @DisplayName("Configuration properties bind successfully with environment / property overrides")
    void testPropertiesBindingAndOverrides() {
        assertEquals("local", storageProperties.getProvider());
        assertEquals("target/custom-storage", storageProperties.getLocal().getBaseDir());
        assertTrue(storageProperties.isDeleteOnUploadFailure());

        assertEquals("test-project-override", gcpProperties.getProjectId());
        assertEquals("override-bucket", gcpStorageProperties.getBucket());
        assertEquals(30, gcpStorageProperties.getSignedUrl().getDurationMinutes());

        assertEquals(500, imageProcessingProperties.getThumbnail().getWidth());
        assertEquals(75, uploadProperties.getMaxFiles());

        assertEquals("http://test-face-service:9000", faceServiceProperties.getBaseUrl());
        assertEquals("http://test-face-service:9000/embed", faceServiceProperties.getEmbedUrl());
    }
}
