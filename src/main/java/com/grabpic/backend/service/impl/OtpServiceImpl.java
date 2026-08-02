package com.grabpic.backend.service.impl;

import com.grabpic.backend.entity.OtpRequest;
import com.grabpic.backend.entity.UserDetails;
import com.grabpic.backend.repository.OtpRequestRepository;
import com.grabpic.backend.repository.UserRepository;
import com.grabpic.backend.security.JwtUtil;
import com.grabpic.backend.service.EmailService;
import com.grabpic.backend.service.OtpService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpRequestRepository otpRequestRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.hardcoded-enabled:false}")
    private boolean hardcodedOtpEnabled;

    @Value("${app.otp.hardcoded-value:123456}")
    private String hardcodedOtpValue;

    @Override
    @Transactional
    public void generateAndSendOtp(String email) {
        // Check user exists and is active
        java.util.Optional<UserDetails> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().getIsActive()) {
            log.info("OTP requested for non-existent or inactive email: {}. Returning success silently.", email);
            return;
        }

        UserDetails user = userOpt.get();

        // Delete any existing OTPs for this email
        otpRequestRepository.deleteAllByEmail(email);

        // Generate OTP (hardcoded if enabled, else random 6-digit)
        String otp = hardcodedOtpEnabled ? hardcodedOtpValue : String.format("%06d", new Random().nextInt(999999));

        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);

        // Save OTP record
        OtpRequest otpRequest = OtpRequest.builder()
                .email(email)
                .otpHash(otpHash)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .attempts(0)
                .build();

        otpRequestRepository.save(otpRequest);

        // Send OTP via email or log if hardcoded mode is enabled
        if (hardcodedOtpEnabled) {
            log.info("=================================================");
            log.info("  HARDCODED OTP ENABLED FOR [{}] : {}", email, otp);
            log.info("=================================================");
        } else {
            emailService.sendOtpEmail(email, otp, user.getFirstname());
        }
    }

    @Override
    @Transactional
    public String verifyOtpAndGenerateToken(String email, String otp) {
        // Check user exists
        UserDetails user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("No account found with this email"));

        // Get latest OTP record
        OtpRequest otpRequest = otpRequestRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() ->
                        new RuntimeException("No OTP requested. Please request a new one"));

        // Check expiry
        if (LocalDateTime.now().isAfter(otpRequest.getExpiryTime())) {
            otpRequestRepository.deleteAllByEmail(email);
            throw new RuntimeException("OTP has expired. Please request a new one");
        }

        // Check max attempts
        if (otpRequest.getAttempts() >= maxAttempts) {
            otpRequestRepository.deleteAllByEmail(email);
            throw new RuntimeException("Too many failed attempts. Please request a new OTP");
        }

        // Validate OTP hash
        if (!passwordEncoder.matches(otp, otpRequest.getOtpHash())) {
            otpRequest.setAttempts(otpRequest.getAttempts() + 1);
            otpRequestRepository.save(otpRequest);

            int remainingAttempts = maxAttempts - otpRequest.getAttempts();
            throw new RuntimeException("Invalid OTP. " + remainingAttempts + " attempts remaining");
        }

        // OTP valid — clean up and issue token
        otpRequestRepository.deleteAllByEmail(email);

        return jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}