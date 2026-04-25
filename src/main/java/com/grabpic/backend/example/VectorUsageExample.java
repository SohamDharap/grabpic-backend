package com.grabpic.backend.example;

import com.grabpic.backend.converter.VectorConverter;
import com.grabpic.backend.entity.FaceEmbeddings;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class VectorUsageExample {

    // D. Example usage (setting and retrieving embeddings)

    public void exampleUsage() {
        // Example 1: Setting embedding in entity
        float[] faceEmbedding = new float[512];
        Arrays.fill(faceEmbedding, 0.1f); // Example embedding data
        
        FaceEmbeddings embeddings = FaceEmbeddings.builder()
                .assetId(123L)
                .embedding(faceEmbedding)  // Clean float[] usage
                .build();
        
        log.info("Created embedding with {} dimensions", embeddings.getEmbedding().length);
        
        // Example 2: Direct converter usage
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        String dbFormat = VectorConverter.convertFloatArrayToVectorString(vector);
        log.info("Database format: {}", dbFormat); // Output: [0.1,0.2,0.3,0.4,0.5]
        
        float[] convertedBack = VectorConverter.convertVectorStringToFloatArray(dbFormat);
        log.info("Converted back: {}", Arrays.toString(convertedBack));
        
        // Example 3: Validation (handled automatically by converter)
        try {
            float[] wrongDimension = new float[256]; // Wrong dimension
            VectorConverter.convertFloatArrayToVectorString(wrongDimension);
        } catch (IllegalArgumentException e) {
            log.info("Validation working: {}", e.getMessage());
        }
    }
    
    // Example 4: Repository usage with native queries
    public void exampleRepositoryUsage() {
        // This shows how the native query still works:
        /*
        @Query(value = "SELECT fe.asset_id, a.asset_url, a.thumbnail_url, " +
                       "fe.embedding <-> :queryVector as distance " +
                       "FROM td_face_embeddings fe " +
                       "JOIN td_asset_details a ON fe.asset_id = a.id " +
                       "WHERE a.event_id = :eventId AND a.is_deleted = false " +
                       "ORDER BY fe.embedding <-> :queryVector LIMIT :limit", 
               nativeQuery = true)
        List<Object[]> findSimilarEmbeddings(@Param("eventId") Long eventId, 
                                           @Param("queryVector") String queryVector, 
                                           @Param("limit") int limit);
        
        // Usage:
        float[] queryEmbedding = extractFaceEmbedding(image);
        String queryVector = VectorConverter.convertFloatArrayToVectorString(queryEmbedding);
        List<Object[]> results = repository.findSimilarEmbeddings(eventId, queryVector, 20);
        */
    }
}
