package com.grabpic.backend.service;

import org.springframework.scheduling.annotation.Async;

public interface EmailService {

    @Async("taskExecutor")
    void sendOtpEmail(String toEmail, String otp, String firstname);
}