package com.grabpic.backend.service;

import com.grabpic.backend.config.properties.ImageProcessingProperties;
import com.grabpic.backend.service.impl.ThumbnailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ThumbnailServiceTest {

    private ImageProcessingProperties properties;
    private ThumbnailServiceImpl thumbnailService;

    @BeforeEach
    void setUp() {
        properties = new ImageProcessingProperties();
        properties.getThumbnail().setEnabled(true);
        properties.getThumbnail().setWidth(200);
        properties.getThumbnail().setHeight(200);
        properties.getThumbnail().setQuality(0.80);
        properties.getThumbnail().setFormat("jpeg");
        properties.getThumbnail().setPreserveAspectRatio(true);

        thumbnailService = new ThumbnailServiceImpl(properties);
    }

    private byte[] createTestImageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Generate thumbnail from JPEG creates scaled down image")
    void testGenerateThumbnail_JpegSuccess() throws IOException {
        byte[] original = createTestImageBytes(800, 600, "jpg");

        byte[] thumbnail = thumbnailService.generateThumbnail(original, "image/jpeg");

        assertNotNull(thumbnail);
        assertTrue(thumbnail.length > 0);

        BufferedImage thumbImage = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertNotNull(thumbImage);
        assertTrue(thumbImage.getWidth() <= 200);
        assertTrue(thumbImage.getHeight() <= 200);
    }

    @Test
    @DisplayName("Generate thumbnail from PNG creates scaled down image")
    void testGenerateThumbnail_PngSuccess() throws IOException {
        byte[] original = createTestImageBytes(1000, 1000, "png");

        byte[] thumbnail = thumbnailService.generateThumbnail(original, "image/png");

        assertNotNull(thumbnail);
        assertTrue(thumbnail.length > 0);

        BufferedImage thumbImage = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertNotNull(thumbImage);
        assertEquals(200, thumbImage.getWidth());
        assertEquals(200, thumbImage.getHeight());
    }

    @Test
    @DisplayName("Generate thumbnail returns null when feature is disabled in config")
    void testGenerateThumbnail_Disabled() throws IOException {
        properties.getThumbnail().setEnabled(false);
        byte[] original = createTestImageBytes(400, 400, "jpg");

        byte[] thumbnail = thumbnailService.generateThumbnail(original, "image/jpeg");

        assertNull(thumbnail);
    }

    @Test
    @DisplayName("Generate thumbnail returns null for corrupt or empty input bytes")
    void testGenerateThumbnail_CorruptInput() {
        byte[] corrupt = new byte[]{1, 2, 3, 4, 5};
        byte[] thumbnail = thumbnailService.generateThumbnail(corrupt, "image/jpeg");
        assertNull(thumbnail);

        assertNull(thumbnailService.generateThumbnail(null, "image/jpeg"));
        assertNull(thumbnailService.generateThumbnail(new byte[0], "image/jpeg"));
    }
}
