package com.sait.peelin.service;

import com.sait.peelin.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WelcomeEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.logo-path:../frontend/static/images/Peelin' Good.png}")
    private String logoPath;

    public void sendWelcomeEmail(User user) {
        if (fromEmail == null || fromEmail.isBlank()) {
            System.out.println("Mail not configured, skipping welcome email for: " + user.getUserEmail());
            return;
        }

        String htmlBody = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <img src="cid:logo" alt="Peelin' Good" style="max-height: 80px; display: block; margin: 0 auto 16px;" />
                <h2 style="color: #703210;">Welcome to Peelin' Good!</h2>
                <p>Hi %s,</p>
                <p>Your account has been created successfully. We're thrilled to have you!</p>
                <a href="%s/menu" style="display: inline-block; background-color: #703210; color: white; padding: 12px 24px; border-radius: 999px; text-decoration: none; font-weight: bold; margin: 16px 0;">
                    Browse Our Menu
                </a>
                <p>If you have any questions, feel free to reach out to us.</p>
                <p style="color: #999; font-size: 12px;">You're receiving this because you created an account at Peelin' Good Bakery.</p>
            </div>
            """.formatted(user.getUsername(), frontendUrl);

        Thread.ofVirtual().start(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, "Peelin' Good Bakery");
                helper.setTo(user.getUserEmail());
                helper.setSubject("Welcome to Peelin' Good!");
                helper.setText(htmlBody, true);

                FileSystemResource logo = new FileSystemResource(logoPath);
                if (logo.exists()) {
                    helper.addInline("logo", logo);
                }

                mailSender.send(message);
                System.out.println("Welcome email sent to: " + user.getUserEmail());
            } catch (Exception e) {
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }
        });
    }
}