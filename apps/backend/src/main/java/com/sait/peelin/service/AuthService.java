package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.AuthResponse;
import com.sait.peelin.dto.v1.auth.ChangePasswordRequest;
import com.sait.peelin.dto.v1.auth.LoginRequest;
import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardTierRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public AuthResponse login(LoginRequest request) {
        String email = Optional.ofNullable(request.getEmail())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);

        String principal = Optional.ofNullable(request.getUsername())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsernameIgnoreCaseOrUserEmailIgnoreCase(principal, principal)
                .orElseThrow();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);

        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        return res;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByUserEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setUserEmail(request.getEmail());
        user.setUserPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserRole(UserRole.customer);
        user.setUserCreatedAt(OffsetDateTime.now());
        user.setActive(true);
        user.setPhotoApprovalPending(false);
        user.setActive(true);
        userRepository.save(user);

        RewardTier lowestTier = rewardTierRepository.findFirstByOrderByRewardTierMinPointsAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No reward tiers configured"));

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setRewardTier(lowestTier);
        customer.setCustomerFirstName(request.getFirstName());
        String mi = request.getMiddleInitial() != null ? request.getMiddleInitial().trim() : null;
        if (mi != null && mi.isEmpty()) {
            mi = null;
        }
        customer.setCustomerMiddleInitial(mi);
        customer.setCustomerLastName(request.getLastName());
        customer.setCustomerPhone(request.getPhone());
        String businessPhone = request.getBusinessPhone();
        if (businessPhone != null && !businessPhone.isBlank()) {
            customer.setCustomerBusinessPhone(businessPhone.trim());
        }
        customer.setCustomerEmail(request.getEmail());
        customer.setCustomerRewardBalance(0);
        customerRepository.save(customer);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);

        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        return res;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User u = currentUserService.requireUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from your current password");
        }
        u.setUserPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(u);
    }
}
