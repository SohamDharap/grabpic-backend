package com.grabpic.backend.service;

public interface AsyncFaceProcessingService {

    /**
     * Asynchronously extracts face embeddings for an uploaded asset from storage
     * and updates its processing status in PostgreSQL.
     *
     * @param assetId   ID of the persisted AssetDetails
     * @param objectKey Storage object key (e.g. events/1/original/photo.jpg)
     */
    void processAssetFaceEmbeddings(Long assetId, String objectKey);
}
