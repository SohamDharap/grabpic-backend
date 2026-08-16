package com.grabpic.backend.service;

import java.nio.file.Path;

public interface AsyncFaceProcessingService {

    /**
     * Asynchronously extracts face embeddings for an uploaded asset
     * and updates its processing status in PostgreSQL.
     *
     * @param assetId  ID of the persisted AssetDetails
     * @param filePath Path on disk where the image file is saved
     */
    void processAssetFaceEmbeddings(Long assetId, Path filePath);
}
