package com.grabpic.backend.controller;

import com.grabpic.backend.dto.request.OtpRequestDto;
import com.grabpic.backend.dto.request.OtpVerifyDto;
import com.grabpic.backend.dto.response.ApiResponseDto;
import com.grabpic.backend.dto.response.JwtResponseDto;
import com.grabpic.backend.entity.UserDetails;
import com.grabpic.backend.repository.UserRepository;
import com.grabpic.backend.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;

    // Step 1 — Request OTP
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponseDto> requestOtp(
            @Valid @RequestBody OtpRequestDto request) {

        log.info("OTP request for email: {}", request.getEmail());
        otpService.generateAndSendOtp(request.getEmail());
        log.info("OTP sent successfully to: {}", request.getEmail());
        return ResponseEntity.ok(
                new ApiResponseDto(true, "OTP sent to " + request.getEmail()));
    }

    // Step 2 — Verify OTP + get JWT
    @PostMapping("/verify-otp")
    public ResponseEntity<JwtResponseDto> verifyOtp(
            @Valid @RequestBody OtpVerifyDto request) {

        log.info("OTP verification attempt for email: {}", request.getEmail());
        String token = otpService.verifyOtpAndGenerateToken(
                request.getEmail(), request.getOtp());

        UserDetails user = userRepository.findByEmail(request.getEmail()).get();

        log.info("OTP verified successfully for email: {}, role: {}", user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(
                new JwtResponseDto(token, user.getEmail(), user.getRole().name()));
    }
}