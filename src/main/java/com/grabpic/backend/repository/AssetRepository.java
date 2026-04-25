package com.grabpic.backend.repository;

import com.grabpic.backend.entity.AssetDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<AssetDetails, Long> {
    
    List<AssetDetails> findByEventIdAndIsDeleted(Long eventId, Boolean isDeleted);
    
    List<AssetDetails> findByPhotographerIdAndIsDeleted(Long photographerId, Boolean isDeleted);
    
    List<AssetDetails> findByEventIdAndIsDeletedAndPhotographerId(Long eventId, Boolean isDeleted, Long photographerId);
}
