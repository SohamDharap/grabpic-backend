package com.grabpic.backend.util;

import com.grabpic.backend.config.properties.GcpStorageProperties;

public final class ObjectKeyGenerator {

    private ObjectKeyGenerator() {
    }

    public static String generateOriginalKey(GcpStorageProperties props, Long eventId, String hashPrefix, String originalFilename) {
        String filename = buildFilename(hashPrefix, originalFilename);
        return String.format("%s/%d/%s/%s",
                cleanPrefix(props.getObjectPrefix()),
                eventId,
                cleanDirectory(props.getOriginalDirectory()),
                filename);
    }

    public static String generateThumbnailKey(GcpStorageProperties props, Long eventId, String hashPrefix, String originalFilename) {
        String filename = buildFilename(hashPrefix, originalFilename);
        return String.format("%s/%d/%s/%s",
                cleanPrefix(props.getObjectPrefix()),
                eventId,
                cleanDirectory(props.getThumbnailDirectory()),
                filename);
    }

    public static String generateEventPrefix(GcpStorageProperties props, Long eventId) {
        return String.format("%s/%d/", cleanPrefix(props.getObjectPrefix()), eventId);
    }

    public static String buildFilename(String hashPrefix, String originalFilename) {
        String safeHash = (hashPrefix != null && hashPrefix.length() >= 12) ? hashPrefix.substring(0, 12) : "img";
        String sanitized = sanitizeFilename(originalFilename);
        return System.currentTimeMillis() + "_" + safeHash + "_" + sanitized;
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        while (sanitized.contains("..")) {
            sanitized = sanitized.replace("..", "_");
        }
        return sanitized.isEmpty() ? "photo.jpg" : sanitized;
    }

    private static String cleanPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "events";
        }
        String clean = prefix.trim();
        while (clean.startsWith("/")) clean = clean.substring(1);
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }

    private static String cleanDirectory(String dir) {
        if (dir == null || dir.isBlank()) {
            return "original";
        }
        String clean = dir.trim();
        while (clean.startsWith("/")) clean = clean.substring(1);
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }
}
