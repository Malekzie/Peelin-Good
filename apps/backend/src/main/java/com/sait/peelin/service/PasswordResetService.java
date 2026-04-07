package com.sait.peelin.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.sait.peelin.model.PasswordResetToken;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.PasswordResetTokenRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.resend.api-key}")
    private String resendApiKey;

    @Value("${app.resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // Token expires after 1 hour
    private static final int EXPIRY_HOURS = 1;

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByUserEmail(email);

        // Always return success even if email doesn't exist — prevents email enumeration
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteAllByUserId(user.getUserId());

        // Generate a secure random token
        String token = generateSecureToken();

        // Save the token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(OffsetDateTime.now().plusHours(EXPIRY_HOURS));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        // Send the email
        sendResetEmail(user, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
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

        // Mark token as used
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


        System.out.println("Attempting to send email to: " + user.getUserEmail());
        System.out.println("Using API key starting with: " + resendApiKey.substring(0, 5));
        System.out.println("Reset link: " + resetLink);
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(user.getUserEmail())
                    .subject("Reset your Peelin' Good password")
                    .html(htmlBody)
                    .build();
            resend.emails().send(emailOptions);
        } catch (ResendException e) {
            System.out.println("Resend error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
