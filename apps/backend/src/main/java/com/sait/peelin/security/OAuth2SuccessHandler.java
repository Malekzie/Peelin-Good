package com.sait.peelin.security;

import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import com.sait.peelin.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration:864000000}")
    private long jwtExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // Extract email and name from the OAuth2 provider
        String email = null;
        String name = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
        } else if (principal instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
        }

        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=no_email");
            return;
        }

        // Find or create the user
        final String finalEmail = email;
        final String finalName = name != null ? name : email.split("@")[0];

        User user = userRepository.findByUserEmail(finalEmail).orElseGet(() -> {
            // Create a new user for first-time OAuth login
            User newUser = new User();
            newUser.setUsername(generateUsername(finalEmail));
            newUser.setUserEmail(finalEmail);
            newUser.setUserPasswordHash(""); // no password for OAuth users
            newUser.setUserRole(UserRole.customer);
            newUser.setUserCreatedAt(OffsetDateTime.now());
            newUser.setActive(true);
            newUser.setPhotoApprovalPending(false);
            userRepository.save(newUser);

            // Create customer record
            RewardTier lowestTier = rewardTierRepository.findFirstByOrderByRewardTierMinPointsAsc()
                    .orElseThrow(() -> new RuntimeException("No reward tiers configured"));

            String[] nameParts = finalName.split(" ", 2);
            Customer customer = new Customer();
            customer.setUser(newUser);
            customer.setRewardTier(lowestTier);
            customer.setCustomerFirstName(nameParts[0]);
            customer.setCustomerLastName(nameParts.length > 1 ? nameParts[1] : "");
            customer.setCustomerEmail(finalEmail);
            customer.setCustomerPhone(null);
            customer.setCustomerRewardBalance(0);
            customerRepository.save(customer);

            return newUser;
        });

        // Generate JWT
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);

        request.getSession().setAttribute("oauth2_pending_token", token);
        response.sendRedirect("/api/v1/auth/oauth2/success");
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }
}
