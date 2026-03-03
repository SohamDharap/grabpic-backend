package com.grabpic.backend.repository;

import com.grabpic.backend.entity.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDetails, Long> {
    Optional<UserDetails> findByEmail(String email);
    boolean existsByEmail(String email);
}