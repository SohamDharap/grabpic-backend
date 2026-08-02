package com.grabpic.backend.repository;

import com.grabpic.backend.entity.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventDetails, Long> {

    // ── Main-event queries ──────────────────────────────────────────────────

    /** Returns only top-level (non-sub) events for a photographer. */
    List<EventDetails> findByPhotographerIdAndIsActiveAndParentEventIdIsNull(Long photographerId, Boolean isActive);

    List<EventDetails> findByPhotographerIdAndIsActive(Long photographerId, Boolean isActive);

    Optional<EventDetails> findByPublicTokenAndIsActive(UUID publicToken, Boolean isActive);

    List<EventDetails> findByPhotographerId(Long photographerId);

    Optional<EventDetails> findByIdAndPhotographerIdAndIsActive(Long id, Long photographerId, Boolean isActive);

    // ── Sub-event queries ───────────────────────────────────────────────────

    /** Returns sub-events that belong to a given parent event and photographer. */
    List<EventDetails> findByParentEventIdAndPhotographerIdAndIsActive(
            Long parentEventId, Long photographerId, Boolean isActive);

    /** Finds a specific sub-event by its ID and parent event ID. */
    Optional<EventDetails> findByIdAndParentEventIdAndPhotographerIdAndIsActive(
            Long id, Long parentEventId, Long photographerId, Boolean isActive);

    /** Check if any sub-events exist for a parent event (used for delete guards). */
    boolean existsByParentEventIdAndIsActive(Long parentEventId, Boolean isActive);

    /** Fetch all sub-events under a parent event regardless of active status (for hard delete). */
    List<EventDetails> findByParentEventId(Long parentEventId);

    Optional<EventDetails> findByIdAndPhotographerId(Long id, Long photographerId);

    Optional<EventDetails> findByIdAndParentEventIdAndPhotographerId(Long id, Long parentEventId, Long photographerId);
}


