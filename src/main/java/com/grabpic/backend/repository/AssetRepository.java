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

    // ── Sub-event-aware queries ──────────────────────────────────────────────

    /** Assets belonging to a specific sub-event within an event. */
    List<AssetDetails> findByEventIdAndSubEventIdAndIsDeleted(Long eventId, Long subEventId, Boolean isDeleted);

    /** Assets in an event that are NOT associated with any sub-event. */
    List<AssetDetails> findByEventIdAndSubEventIdIsNullAndIsDeleted(Long eventId, Boolean isDeleted);

    List<AssetDetails> findByEventId(Long eventId);

    List<AssetDetails> findBySubEventId(Long subEventId);

    List<AssetDetails> findByEventIdAndSubEventId(Long eventId, Long subEventId);

    java.util.Optional<AssetDetails> findByIdAndEventId(Long id, Long eventId);

    long countByEventId(Long eventId);

    long countBySubEventId(Long subEventId);
}


