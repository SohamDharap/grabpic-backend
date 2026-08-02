package com.grabpic.backend.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Shared file validation utilities used across UploadController, PublicSearchController,
 * and UploadServiceImpl.
 */
public final class FileValidationUtil {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    private FileValidationUtil() {
        // utility class
    }

    /**
     * Returns true if the file has a supported image content type.
     */
    public static boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Returns true if the filename has a supported image extension.
     * Used when processing extracted ZIP entries that may not carry a content-type.
     */
    public static boolean hasValidImageExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String lower = filename.toLowerCase();
        return ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * Returns true if the file is a ZIP archive.
     */
    public static boolean isZipFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean mimeMatch = contentType != null && (
                contentType.equalsIgnoreCase("application/zip") ||
                contentType.equalsIgnoreCase("application/x-zip-compressed") ||
                contentType.equalsIgnoreCase("application/octet-stream")
        );
        boolean extMatch = filename != null && filename.toLowerCase().endsWith(".zip");
        return mimeMatch && extMatch;
    }

    /**
     * Zip Slip guard: verifies that a resolved entry path is strictly inside the base directory.
     * Throws IllegalArgumentException if the entry would escape the base directory.
     *
     * @param entryPath the path of the ZIP entry resolved against baseDir
     * @param baseDir   the temp directory used for extraction
     */
    public static void validateZipEntryPath(Path entryPath, Path baseDir) throws IOException {
        Path canonical = entryPath.toAbsolutePath().normalize();
        Path canonicalBase = baseDir.toAbsolutePath().normalize();
        if (!canonical.startsWith(canonicalBase)) {
            throw new IllegalArgumentException(
                    "Zip Slip detected: entry would escape extraction directory: " + entryPath);
        }
    }
}
