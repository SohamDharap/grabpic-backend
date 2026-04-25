package com.grabpic.backend.service.impl;

import com.grabpic.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String firstname) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("GrabPic — Your Login OTP");
        message.setText(
                "Hi " + firstname + ",\\n\\n" +
                        "Your GrabPic login OTP is: " + otp + "\\n\\n" +
                        "This OTP is valid for 10 minutes.\\n" +
                        "Do not share this with anyone.\\n\\n" +
                        "— GrabPic Team"
        );
        mailSender.send(message);
    }
}