package com.grabpic.backend.repository;

import com.grabpic.backend.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface OtpRequestRepository extends JpaRepository<OtpRequest, Long> {

    // Get latest OTP for email
    Optional<OtpRequest> findTopByEmailOrderByCreatedAtDesc(String email);

    // Delete all OTPs for email (called after successful verification)
    @Modifying
    @Query("DELETE FROM OtpRequest o WHERE o.email = :email")
    void deleteAllByEmail(String email);
}