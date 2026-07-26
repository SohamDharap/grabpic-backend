package com.grabpic.backend.service.impl;

import com.grabpic.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.otp.hardcoded-enabled:false}")
    private boolean hardcodedOtpEnabled;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String firstname) {
        if (hardcodedOtpEnabled) {
            log.info("Hardcoded OTP mode active. Skipping email dispatch for {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("GrabPic — Your Login OTP");
            message.setText(
                    "Hi " + (firstname != null ? firstname : "User") + ",\n\n" +
                            "Your GrabPic login OTP is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n" +
                            "Do not share this with anyone.\n\n" +
                            "— GrabPic Team"
            );
            mailSender.send(message);
            log.info("Successfully sent OTP email to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send email via SMTP ({}), falling back to console log.", e.getMessage());
            log.info("=================================================");
            log.info("  DEV OTP FOR [{}] : {}", toEmail, otp);
            log.info("=================================================");
        }
    }
}