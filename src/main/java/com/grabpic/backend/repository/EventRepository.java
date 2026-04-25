package com.grabpic.backend.repository;

import com.grabpic.backend.entity.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventDetails, Long> {
    
    List<EventDetails> findByPhotographerIdAndIsActive(Long photographerId, Boolean isActive);
    
    Optional<EventDetails> findByPublicTokenAndIsActive(UUID publicToken, Boolean isActive);
    
    List<EventDetails> findByPhotographerId(Long photographerId);
}
