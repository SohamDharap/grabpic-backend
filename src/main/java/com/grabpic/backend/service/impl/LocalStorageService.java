package com.grabpic.backend.service.impl;

import com.grabpic.backend.config.properties.StorageProperties;
import com.grabpic.backend.service.StorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;

@Slf4j
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    private Path getBasePath() {
        return Paths.get(storageProperties.getLocal().getBaseDir());
    }

    private Path resolvePath(String objectKey) {
        return getBasePath().resolve(objectKey).normalize();
    }

    @Override
    public String upload(String objectKey, byte[] bytes, String contentType) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("ObjectKey cannot be null or blank");
        }
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }

        try {
            Path targetPath = resolvePath(objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Uploaded local file: {}", targetPath);
            return objectKey;
        } catch (IOException e) {
            log.error("Failed to write local file: key={}, error={}", objectKey, e.getMessage());
            throw new RuntimeException("Local file storage failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("ObjectKey cannot be null or blank");
        }

        try {
            Path targetPath = resolvePath(objectKey);
            if (!Files.exists(targetPath)) {
                throw new NoSuchFileException("File not found: " + targetPath);
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error("Failed to read local file: key={}, error={}", objectKey, e.getMessage());
            throw new RuntimeException("Local file read failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration duration) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("ObjectKey cannot be null or blank");
        }
        // Local fallback: returns local simulated URL
        return "/storage/" + objectKey;
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            Path targetPath = resolvePath(objectKey);
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.info("Deleted local file: {}", targetPath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete local file: key={}, error={}", objectKey, e.getMessage());
        }
    }

    @Override
    public void deletePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        Path targetDir = resolvePath(prefix);
        if (!Files.exists(targetDir)) {
            return;
        }

        try {
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("Deleted local directory tree: {}", targetDir);
        } catch (IOException e) {
            log.warn("Failed to delete local prefix directory {}: {}", prefix, e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        return Files.exists(resolvePath(objectKey));
    }
}
