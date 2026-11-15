package com.grabpic.backend.service.impl;

import com.grabpic.backend.config.properties.ImageProcessingProperties;
import com.grabpic.backend.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailServiceImpl implements ThumbnailService {

    private final ImageProcessingProperties imageProcessingProperties;

    @Override
    public byte[] generateThumbnail(byte[] originalImage, String contentType) {
        if (originalImage == null || originalImage.length == 0) {
            log.warn("Cannot generate thumbnail: original image is empty or null.");
            return null;
        }

        ImageProcessingProperties.ThumbnailProperties props = imageProcessingProperties.getThumbnail();
        if (!props.isEnabled()) {
            log.debug("Thumbnail generation is disabled by configuration.");
            return null;
        }

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(originalImage);
            BufferedImage bufferedImage = ImageIO.read(in);
            if (bufferedImage == null) {
                log.warn("ImageIO unable to decode image for thumbnail generation (contentType={})", contentType);
                return null;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String outputFormat = props.getFormat() != null ? props.getFormat().toLowerCase().replace("jpg", "jpeg") : "jpeg";

            Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(bufferedImage)
                    .size(props.getWidth(), props.getHeight())
                    .keepAspectRatio(props.isPreserveAspectRatio())
                    .outputFormat(outputFormat);

            if ("jpeg".equalsIgnoreCase(outputFormat) || "jpg".equalsIgnoreCase(outputFormat)) {
                builder.outputQuality(props.getQuality());
            }

            builder.toOutputStream(out);
            byte[] thumbnailBytes = out.toByteArray();
            log.info("Generated thumbnail: {} bytes (original: {} bytes, format={})",
                    thumbnailBytes.length, originalImage.length, outputFormat);
            return thumbnailBytes;
        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            // Safe fallback: do not fail upload if thumbnail fails, return null
            return null;
        }
    }
}
