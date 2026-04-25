package com.grabpic.backend.repository;

import com.grabpic.backend.entity.UserDetails;
import com.grabpic.backend.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDetails, Long> {
    Optional<UserDetails> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserDetails> findByRole(UserRole role);
}