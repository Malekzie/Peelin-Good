package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.AccountProfilePatchRequest;
import com.sait.peelin.dto.v1.auth.AuthResponse;
import com.sait.peelin.dto.v1.auth.ChangePasswordRequest;
import com.sait.peelin.dto.v1.auth.LoginRequest;
import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.UserRepository;
import com.sait.peelin.support.GuestContactFiller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    /** Practical RFC 5322–oriented check aligned with mobile app validation. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
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

        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        String emailNorm = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUserEmail(emailNorm)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        boolean priorGuestCheckout = !customerRepository.findGuestCustomersByEmailNormalized(emailNorm).isEmpty();
        if (!priorGuestCheckout && StringUtils.hasText(request.getPhone())) {
            String digits = GuestContactFiller.normalizeDigits(request.getPhone());
            if (digits.length() >= 10
                    && !customerRepository.findGuestCustomerIdsByPhoneDigits(digits).isEmpty()) {
                priorGuestCheckout = true;
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setUserEmail(emailNorm);
        user.setUserPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserRole(UserRole.customer);
        user.setUserCreatedAt(OffsetDateTime.now());
        user.setActive(true);
        user.setPhotoApprovalPending(false);
        user.setActive(true);
        userRepository.save(user);

        customerService.createRegisteredCustomer(user, request.getPhone());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();

        String token = jwtService.generateToken(userDetails);
        AuthResponse res = buildAuthResponse(user, token);
        res.setPriorGuestCheckout(priorGuestCheckout);
        if (priorGuestCheckout) {
            res.setGuestProfileCompletionMessage(
                    "You have previously checked out as a guest. Please complete the remaining information to finish your registration.");
        }
        return res;
    }

    /**
     * Updates username and/or sign-in email for the current user. Returns a fresh JWT
     * (subject is username). Syncs customer profile email when applicable.
     */
    @Transactional
    public AuthResponse patchMyProfile(AccountProfilePatchRequest req) {
        if (req.getUsername() == null && req.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }
        User u = currentUserService.requireUser();
        boolean dirty = false;

        if (req.getUsername() != null) {
            String nu = req.getUsername().trim();
            if (nu.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
            }
            if (!USERNAME_PATTERN.matcher(nu).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
            }
            if (!nu.equalsIgnoreCase(u.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCaseAndUserIdNot(nu, u.getUserId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
                }
                u.setUsername(nu);
                dirty = true;
            }
        }

        if (req.getEmail() != null) {
            String ne = req.getEmail().trim().toLowerCase();
            if (ne.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
            }
            if (ne.length() > 254 || !EMAIL_PATTERN.matcher(ne).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
            }
            if (!ne.equalsIgnoreCase(u.getUserEmail())) {
                if (userRepository.existsByUserEmailIgnoreCaseAndUserIdNot(ne, u.getUserId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                }
                u.setUserEmail(ne);
                dirty = true;
                Optional<Customer> cust = customerRepository.findByUser_UserId(u.getUserId());
                if (cust.isPresent()) {
                    Customer c = cust.get();
                    c.setCustomerEmail(ne);
                    customerRepository.save(c);
                }
            }
        }

        if (dirty) {
            userRepository.save(u);
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getUserPasswordHash())
                .authorities("ROLE_" + u.getUserRole().name().toUpperCase())
                .build();
        String token = jwtService.generateToken(userDetails);
        return buildAuthResponse(u, token);
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

    public AuthResponse getUserInfoFromToken(String token) {
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow();

        AuthResponse res = new AuthResponse();
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        return res;
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        res.setEmail(user.getUserEmail());
        return res;
    }
}
