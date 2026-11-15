package com.grabpic.backend.service;

public interface ThumbnailService {

    /**
     * Generates a compressed and resized thumbnail byte array from the original image.
     *
     * @param originalImage original image bytes
     * @param contentType   original MIME content type
     * @return thumbnail bytes, or null if disabled or if processing failed
     */
    byte[] generateThumbnail(byte[] originalImage, String contentType);
}
