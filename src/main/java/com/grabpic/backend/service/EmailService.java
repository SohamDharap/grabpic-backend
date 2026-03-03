package com.grabpic.backend.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String firstname);
}