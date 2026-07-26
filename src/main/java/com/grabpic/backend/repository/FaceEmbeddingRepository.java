package com.grabpic.backend.repository;

import com.grabpic.backend.entity.FaceEmbeddings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbeddings, Long> {
    
    List<FaceEmbeddings> findByAssetId(Long assetId);
    
    @Query("SELECT fe FROM FaceEmbeddings fe JOIN AssetDetails a ON fe.assetId = a.id WHERE a.eventId = :eventId AND a.isDeleted = false")
    List<FaceEmbeddings> findByEventId(@Param("eventId") Long eventId);

    // Native insert query using explicit CAST(:embedding AS vector) for pgvector compatibility
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO td_face_embeddings (asset_id, embedding, created_at) VALUES (:assetId, CAST(:embedding AS vector), CURRENT_TIMESTAMP)", nativeQuery = true)
    void insertFaceEmbedding(@Param("assetId") Long assetId, @Param("embedding") String embedding);
    
    // Vector similarity search using pgvector Cosine Distance operator (<=>) with explicit CAST
    @Query(value = "SELECT fe.asset_id, a.asset_url, a.thumbnail_url, (fe.embedding <=> CAST(:queryVector AS vector)) as distance " +
                   "FROM td_face_embeddings fe " +
                   "JOIN td_asset_details a ON fe.asset_id = a.id " +
                   "WHERE a.event_id = :eventId AND a.is_deleted = false " +
                   "ORDER BY fe.embedding <=> CAST(:queryVector AS vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarEmbeddings(@Param("eventId") Long eventId, 
                                       @Param("queryVector") String queryVector, 
                                       @Param("limit") int limit);
}
