package com.sait.peelin.service;

import com.sait.peelin.model.PasswordResetToken;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.PasswordResetTokenRepository;
import com.sait.peelin.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(User user, String token) {
        if (fromEmail == null || fromEmail.isBlank()) {
            System.err.println("Mail not configured, skipping password reset email");
            return;
        }

        String resetLink = frontendUrl + "/login/reset-password?token=" + token;

        String htmlBody = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #703210;">Reset Your Password</h2>
                <p>Hi %s,</p>
                <p>We received a request to reset your password for your Peelin' Good account.</p>
                <p>Click the button below to reset your password. This link expires in %d hour(s).</p>
                <a href="%s" style="display: inline-block; background-color: #703210; color: white; padding: 12px 24px; border-radius: 999px; text-decoration: none; font-weight: bold; margin: 16px 0;">
                    Reset Password
                </a>
                <p>If you didn't request a password reset, you can safely ignore this email.</p>
                <p style="color: #999; font-size: 12px;">This link will expire in %d hour(s).</p>
            </div>
            """.formatted(user.getUsername(), EXPIRY_HOURS, resetLink, EXPIRY_HOURS);

        Thread.ofVirtual().start(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(fromEmail, "Peelin' Good Bakery");
                helper.setTo(user.getUserEmail());
                helper.setSubject("Reset your Peelin' Good password");
                helper.setText(htmlBody, true);
                mailSender.send(message);
                System.out.println("Password reset email sent to: " + user.getUserEmail());
            } catch (Exception e) {
                System.err.println("Failed to send password reset email: " + e.getMessage());
            }
        });
    }
}