package com.sait.peelin.service;

import com.sait.peelin.model.PasswordResetToken;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.PasswordResetTokenRepository;
import com.sait.peelin.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.core.io.FileSystemResource;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.logo-path:../frontend/static/images/Peelin' Good.png}")
    private String logoPath;

    private static final int EXPIRY_HOURS = 1;

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByUserEmail(email);

        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        tokenRepository.deleteAllByUserId(user.getUserId());

        String token = generateSecureToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(hashToken(token));
        resetToken.setExpiresAt(OffsetDateTime.now().plusHours(EXPIRY_HOURS));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        sendResetEmail(user, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setUserPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    // generates a random number from random values from the OS and stores them in a 32 byte array of numbers.
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        // converts the array of bytes into a readable string that is compatible with URLS
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(User user, String token) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail not configured (spring.mail.username unset); skipping password reset email for userId={}", user.getUserId());
            return;
        }

        String resetLink = frontendUrl + "/login/reset-password?token=" + token;

        String htmlBody = """
    <!DOCTYPE html><html><head><meta charset='UTF-8'></head>
    <body style='margin:0;padding:0;background:#f5f0eb;font-family:Arial,Helvetica,sans-serif;'>
    <table width='100%%' cellpadding='0' cellspacing='0' style='background:#f5f0eb;padding:32px 0;'>
    <tr><td align='center'>
    <table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%%;'>

    <tr><td style='background:#5c3d2e;padding:32px 40px;border-radius:8px 8px 0 0;text-align:center;'>
    <img src='cid:logo' alt="Peelin' Good" style='max-height:80px;max-width:240px;display:block;margin:0 auto 12px;' />
    <p style='margin:8px 0 0;color:#e0cfc4;font-size:14px;'>Reset your password</p>
    </td></tr>

    <tr><td style='background:#fff;padding:40px;'>
    <p style='margin:0 0 24px;font-size:16px;color:#333;'>Hi %s,</p>
    <p style='margin:0 0 16px;font-size:15px;color:#555;line-height:1.6;'>
    We received a request to reset your password for your Peelin&apos; Good account.</p>
    <p style='margin:0 0 32px;font-size:15px;color:#555;line-height:1.6;'>
    Click the button below to reset your password. This link expires in %d hour(s).</p>

    <div style='text-align:center;margin-bottom:32px;'>
    <a href='%s' style='display:inline-block;background:#5c3d2e;color:#fff;text-decoration:none;
    font-size:14px;font-weight:bold;padding:12px 32px;border-radius:6px;'>Reset Password</a>
    </div>

    <p style='margin:0 0 16px;font-size:13px;color:#555;line-height:1.6;'>
    If you didn&apos;t request a password reset, you can safely ignore this email.</p>
    <p style='margin:0;font-size:12px;color:#999;'>This link will expire in %d hour(s).</p>
    </td></tr>

    <tr><td style='background:#e8e0d8;padding:16px 40px;border-radius:0 0 8px 8px;text-align:center;'>
    <p style='margin:0;font-size:12px;color:#888;'>
    &copy; Peelin&apos; Good &mdash; This email was sent to %s because you requested a password reset.
    </p></td></tr>

    </table></td></tr></table>
    </body></html>
    """.formatted(user.getUsername(), EXPIRY_HOURS, resetLink, EXPIRY_HOURS, user.getUserEmail());

        Thread.ofVirtual().start(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, "Peelin' Good Bakery");
                helper.setTo(user.getUserEmail());
                helper.setSubject("Reset your Peelin' Good password");
                helper.setText(htmlBody, true);

                FileSystemResource logo = new FileSystemResource(logoPath);
                if (logo.exists()) {
                    helper.addInline("logo", logo);
                }

                mailSender.send(message);
                log.info("Password reset email sent to userId={}", user.getUserId());
            } catch (Exception e) {
                log.error("Failed to send password reset email for userId={}", user.getUserId(), e);
            }
        });
    }

    public void validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has expired");
        }
    }
}