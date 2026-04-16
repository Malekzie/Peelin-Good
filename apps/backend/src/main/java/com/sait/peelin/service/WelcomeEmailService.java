package com.sait.peelin.service;

import com.sait.peelin.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WelcomeEmailService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.logo-path:../frontend/static/images/Peelin' Good.png}")
    private String logoPath;

    public void sendWelcomeEmail(User user) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured (spring.mail.username unset); skipping welcome email for userId={}", user.getUserId());
            return;
        }

        String htmlBody = """
    <!DOCTYPE html><html><head><meta charset='UTF-8'></head>
    <body style='margin:0;padding:0;background:#f5f0eb;font-family:Arial,Helvetica,sans-serif;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background:#f5f0eb;padding:32px 0;'>
    <tr><td align='center'>
    <table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%%;'>

    <tr><td style='background:#5c3d2e;padding:32px 40px;border-radius:8px 8px 0 0;text-align:center;'>
    <img src='cid:logo' alt="Peelin' Good" style='max-height:80px;max-width:240px;display:block;margin:0 auto 12px;' />
    <p style='margin:8px 0 0;color:#e0cfc4;font-size:14px;'>Welcome to Peelin&apos; Good!</p>
    </td></tr>

    <tr><td style='background:#fff;padding:40px;'>
    <p style='margin:0 0 24px;font-size:16px;color:#333;'>Hi %s,</p>
    <p style='margin:0 0 16px;font-size:15px;color:#555;line-height:1.6;'>
    Your account has been created successfully. We&apos;re thrilled to have you!</p>
    <p style='margin:0 0 32px;font-size:15px;color:#555;line-height:1.6;'>
    Browse our fresh baked goods and place your first order today.</p>

    <div style='text-align:center;margin-bottom:32px;'>
    <a href='%s/menu' style='display:inline-block;background:#5c3d2e;color:#fff;text-decoration:none;
    font-size:14px;font-weight:bold;padding:12px 32px;border-radius:6px;'>Browse Our Menu &rarr;</a>
    </div>

    <p style='margin:0;font-size:13px;color:#999;line-height:1.6;'>
    If you have any questions, feel free to reach out to us.</p>
    </td></tr>

    <tr><td style='background:#e8e0d8;padding:16px 40px;border-radius:0 0 8px 8px;text-align:center;'>
    <p style='margin:0;font-size:12px;color:#888;'>
    &copy; Peelin&apos; Good &mdash; You&apos;re receiving this because you created an account.
    </p></td></tr>

    </table></td></tr></table>
    </body></html>
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
                log.info("Welcome email sent to userId={}", user.getUserId());
            } catch (Exception e) {
                log.error("Failed to send welcome email for userId={}: {}", user.getUserId(), e.getMessage());
            }
        });
    }
}