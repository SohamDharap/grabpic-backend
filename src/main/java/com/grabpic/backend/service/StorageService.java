package com.grabpic.backend.service;

import java.time.Duration;

public interface StorageService {

    /**
     * Uploads raw bytes to the storage provider under the specified object key.
     *
     * @param objectKey   the destination path / key within the bucket or base directory
     * @param bytes       the binary payload to store
     * @param contentType MIME content type (e.g. "image/jpeg", "image/png")
     * @return the persisted objectKey
     */
    String upload(String objectKey, byte[] bytes, String contentType);

    /**
     * Downloads all bytes for the given object key from storage.
     *
     * @param objectKey the object key to retrieve
     * @return the binary payload
     */
    byte[] download(String objectKey);

    /**
     * Generates a temporary signed URL for client access to a private object.
     * For local storage, returns a local URL endpoint.
     *
     * @param objectKey the object key
     * @param duration  the validity period
     * @return time-limited signed URL string
     */
    String generateSignedUrl(String objectKey, Duration duration);

    /**
     * Deletes a single object from storage.
     *
     * @param objectKey the object key to remove
     */
    void delete(String objectKey);

    /**
     * Deletes all objects sharing the specified prefix (e.g. all photos under an event).
     *
     * @param prefix the key prefix (e.g. "events/123/")
     */
    void deletePrefix(String prefix);

    /**
     * Checks if an object exists in storage.
     *
     * @param objectKey the object key to check
     * @return true if object exists, false otherwise
     */
    boolean exists(String objectKey);
}
