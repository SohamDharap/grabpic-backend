package com.grabpic.backend.service.impl;

import com.grabpic.backend.service.EmailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.otp.hardcoded-enabled:false}")
    private boolean hardcodedOtpEnabled;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:GrabPic Admin}")
    private String fromName;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String firstname) {
        if (hardcodedOtpEnabled) {
            log.info("Hardcoded OTP mode active. Skipping email dispatch for {}", toEmail);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(toEmail);
            helper.setSubject("GrabPic — Your Login OTP");
            helper.setText(
                    "Hi " + (firstname != null ? firstname : "User") + ",\n\n" +
                            "Your GrabPic login OTP is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n" +
                            "Do not share this with anyone.\n\n" +
                            "— GrabPic Team"
            );

            mailSender.send(mimeMessage);
            log.info("Successfully sent OTP email to {} with sender name '{}'", toEmail, fromName);
        } catch (Exception e) {
            log.warn("Failed to send email via SMTP ({}), falling back to console log.", e.getMessage());
            log.info("=================================================");
            log.info("  DEV OTP FOR [{}] : {}", toEmail, otp);
            log.info("=================================================");
        }
    }
}