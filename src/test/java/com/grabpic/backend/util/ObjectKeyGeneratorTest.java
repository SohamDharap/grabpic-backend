package com.grabpic.backend.util;

import com.grabpic.backend.config.properties.GcpStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectKeyGeneratorTest {

    private GcpStorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GcpStorageProperties();
        properties.setObjectPrefix("events");
        properties.setOriginalDirectory("original");
        properties.setThumbnailDirectory("thumbnail");
    }

    @Test
    @DisplayName("Generate original key matches pattern: events/{eventId}/original/{timestamp}_{hash}_{filename}")
    void testGenerateOriginalKey() {
        Long eventId = 42L;
        String hashPrefix = "a1b2c3d4e5f67890";
        String filename = "wedding photo.jpg";

        String key = ObjectKeyGenerator.generateOriginalKey(properties, eventId, hashPrefix, filename);

        assertNotNull(key);
        assertTrue(key.startsWith("events/42/original/"));
        assertTrue(key.contains("_a1b2c3d4e5f6_wedding_photo.jpg"));
    }

    @Test
    @DisplayName("Generate thumbnail key matches pattern: events/{eventId}/thumbnail/{timestamp}_{hash}_{filename}")
    void testGenerateThumbnailKey() {
        Long eventId = 42L;
        String hashPrefix = "a1b2c3d4e5f67890";
        String filename = "wedding_photo.jpg";

        String key = ObjectKeyGenerator.generateThumbnailKey(properties, eventId, hashPrefix, filename);

        assertNotNull(key);
        assertTrue(key.startsWith("events/42/thumbnail/"));
        assertTrue(key.contains("_a1b2c3d4e5f6_wedding_photo.jpg"));
    }

    @Test
    @DisplayName("Generate event prefix produces clean directory prefix")
    void testGenerateEventPrefix() {
        String prefix = ObjectKeyGenerator.generateEventPrefix(properties, 100L);
        assertEquals("events/100/", prefix);
    }

    @Test
    @DisplayName("Sanitize filename removes unsafe path traversal characters")
    void testSanitizeFilename() {
        assertEquals("photo.jpg", ObjectKeyGenerator.sanitizeFilename("photo.jpg"));
        assertEquals("photo__123.jpg", ObjectKeyGenerator.sanitizeFilename("photo /123.jpg"));
        assertEquals("test__photo.png", ObjectKeyGenerator.sanitizeFilename("test/..photo.png"));
        assertEquals("photo.jpg", ObjectKeyGenerator.sanitizeFilename(null));
        assertEquals("photo.jpg", ObjectKeyGenerator.sanitizeFilename(""));
    }
}
