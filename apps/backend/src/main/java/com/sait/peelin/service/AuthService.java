package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.AuthResponse;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsernameOrUserEmail(request.getUsername(), request.getEmail()).orElseThrow();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getUserRole().name());
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
        userRepository.save(user);

        RewardTier lowestTier = rewardTierRepository.findFirstByOrderByRewardTierMinPointsAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No reward tiers configured"));

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setRewardTier(lowestTier);
        customer.setCustomerFirstName(request.getFirstName());
        customer.setCustomerLastName(request.getLastName());
        customer.setCustomerPhone(request.getPhone());
        customer.setCustomerEmail(request.getEmail());
        customer.setCustomerRewardBalance(0);
        customer.setPhotoApprovalPending(false);
        customerRepository.save(customer);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getUserRole().name());
    }
}
