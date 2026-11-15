package com.grabpic.backend.service.impl;

import com.google.api.gax.paging.Page;
import com.google.auth.ServiceAccountSigner;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.storage.*;
import com.grabpic.backend.config.properties.GcpProperties;
import com.grabpic.backend.config.properties.GcpStorageProperties;
import com.grabpic.backend.service.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GcsStorageService implements StorageService {

    private final GcpStorageProperties storageProperties;
    private final GcpProperties gcpProperties;
    private Storage storage;
    private IamCredentialsClient iamCredentialsClient;
    private ServiceAccountSigner serviceAccountSigner;

    public GcsStorageService(GcpStorageProperties storageProperties, GcpProperties gcpProperties) {
        this.storageProperties = storageProperties;
        this.gcpProperties = gcpProperties;
    }

    // Constructor for testing with mocked Storage & Signer
    public GcsStorageService(GcpStorageProperties storageProperties,
                             GcpProperties gcpProperties,
                             Storage storage,
                             ServiceAccountSigner serviceAccountSigner) {
        this.storageProperties = storageProperties;
        this.gcpProperties = gcpProperties;
        this.storage = storage;
        this.serviceAccountSigner = serviceAccountSigner;
    }

    @PostConstruct
    public void initialize() {
        if (this.storage == null) {
            StorageOptions.Builder optionsBuilder = StorageOptions.newBuilder();
            if (gcpProperties != null && gcpProperties.getProjectId() != null && !gcpProperties.getProjectId().isBlank()) {
                optionsBuilder.setProjectId(gcpProperties.getProjectId().trim());
            }
            this.storage = optionsBuilder.build().getService();
            log.info("Initialized Google Cloud Storage client with ADC for bucket '{}'", storageProperties.getBucket());
        }

        if (this.serviceAccountSigner == null && storageProperties.getSignedUrl().isEnabled()) {
            try {
                this.iamCredentialsClient = IamCredentialsClient.create();
                String saEmail = storageProperties.getSignedUrl().getSigningServiceAccount();
                this.serviceAccountSigner = new IamServiceAccountSigner(this.iamCredentialsClient, saEmail);
                log.info("Initialized IAM Credentials Signer for service account: {}", saEmail);
            } catch (IOException e) {
                log.warn("Could not initialize IamCredentialsClient at startup: {}. Signed URLs will fall back if called.", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        if (iamCredentialsClient != null) {
            try {
                iamCredentialsClient.close();
            } catch (Exception e) {
                log.warn("Error closing IamCredentialsClient: {}", e.getMessage());
            }
        }
    }

    @Override
    public String upload(String objectKey, byte[] bytes, String contentType) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("GCS objectKey cannot be null or blank");
        }
        if (bytes == null) {
            throw new IllegalArgumentException("Payload bytes cannot be null");
        }

        String bucketName = storageProperties.getBucket();
        String resolvedContentType = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";

        log.info("Uploading object to GCS: bucket='{}', key='{}', size={} bytes, contentType='{}'",
                bucketName, objectKey, bytes.length, resolvedContentType);

        BlobId blobId = BlobId.of(bucketName, objectKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(resolvedContentType)
                .build();

        storage.create(blobInfo, bytes);
        log.info("Successfully uploaded object to GCS: key='{}'", objectKey);
        return objectKey;
    }

    @Override
    public byte[] download(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("GCS objectKey cannot be null or blank");
        }

        String bucketName = storageProperties.getBucket();
        log.debug("Downloading object from GCS: bucket='{}', key='{}'", bucketName, objectKey);

        try {
            byte[] content = storage.readAllBytes(bucketName, objectKey);
            log.debug("Successfully downloaded object from GCS: key='{}', size={} bytes", objectKey, content.length);
            return content;
        } catch (StorageException e) {
            log.error("Failed to download object from GCS: bucket='{}', key='{}', error={}",
                    bucketName, objectKey, e.getMessage());
            throw new RuntimeException("GCS download failed for key: " + objectKey, e);
        }
    }

    @Override
    public String generateSignedUrl(String objectKey, Duration duration) {
        if (!storageProperties.getSignedUrl().isEnabled()) {
            log.warn("Signed URL generation is disabled in configuration.");
            return null;
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("GCS objectKey cannot be null or blank");
        }

        int maxDurationMinutes = storageProperties.getSignedUrl().getMaxDurationMinutes();
        long requestedMinutes = (duration != null && !duration.isNegative())
                ? duration.toMinutes()
                : storageProperties.getSignedUrl().getDurationMinutes();

        long effectiveMinutes = Math.min(Math.max(1, requestedMinutes), maxDurationMinutes);
        String bucketName = storageProperties.getBucket();

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectKey).build();

        try {
            Storage.SignUrlOption[] signOptions;
            if (this.serviceAccountSigner != null) {
                signOptions = new Storage.SignUrlOption[]{
                        Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                        Storage.SignUrlOption.withV4Signature(),
                        Storage.SignUrlOption.signWith(this.serviceAccountSigner)
                };
            } else {
                signOptions = new Storage.SignUrlOption[]{
                        Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                        Storage.SignUrlOption.withV4Signature()
                };
            }

            URL signedUrl = storage.signUrl(blobInfo, effectiveMinutes, TimeUnit.MINUTES, signOptions);
            String urlString = signedUrl.toString();

            // Mask signed URL in logs for security
            String maskedUrl = urlString.length() > 50 ? urlString.substring(0, 50) + "..." : urlString;
            log.debug("Generated V4 Signed URL for key='{}', expires in {} minutes: {}", objectKey, effectiveMinutes, maskedUrl);

            return urlString;
        } catch (Exception e) {
            log.error("Failed to generate V4 signed URL for key='{}': {}", objectKey, e.getMessage());
            throw new RuntimeException("Failed to generate signed URL for key: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        String bucketName = storageProperties.getBucket();
        log.info("Deleting object from GCS: bucket='{}', key='{}'", bucketName, objectKey);
        try {
            boolean deleted = storage.delete(bucketName, objectKey);
            if (deleted) {
                log.info("Successfully deleted object from GCS: key='{}'", objectKey);
            } else {
                log.warn("Object not found for deletion in GCS: key='{}'", objectKey);
            }
        } catch (StorageException e) {
            log.warn("Error deleting object from GCS: key='{}', error={}", objectKey, e.getMessage());
        }
    }

    @Override
    public void deletePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }

        String bucketName = storageProperties.getBucket();
        log.info("Deleting objects by prefix from GCS: bucket='{}', prefix='{}'", bucketName, prefix);

        try {
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(prefix));
            List<BlobId> blobIds = new ArrayList<>();
            for (Blob blob : blobs.iterateAll()) {
                blobIds.add(blob.getBlobId());
            }

            if (!blobIds.isEmpty()) {
                List<Boolean> results = storage.delete(blobIds);
                long deletedCount = results.stream().filter(Boolean::booleanValue).count();
                log.info("Deleted {} objects under prefix '{}' in bucket '{}'", deletedCount, prefix, bucketName);
            } else {
                log.info("No objects found under prefix '{}' to delete", prefix);
            }
        } catch (StorageException e) {
            log.warn("Error executing prefix deletion in GCS for prefix '{}': {}", prefix, e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        try {
            Blob blob = storage.get(storageProperties.getBucket(), objectKey,
                    Storage.BlobGetOption.fields(Storage.BlobField.NAME));
            return blob != null && blob.exists();
        } catch (StorageException e) {
            log.warn("Error checking existence of object in GCS: key='{}', error={}", objectKey, e.getMessage());
            return false;
        }
    }
}
