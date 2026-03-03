package com.grabpic.backend.service;

public interface OtpService {
    void generateAndSendOtp(String email);
    String verifyOtpAndGenerateToken(String email, String otp);
}