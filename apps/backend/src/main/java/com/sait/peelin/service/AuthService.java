package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.AccountProfilePatchRequest;
import com.sait.peelin.dto.v1.auth.AuthResponse;
import com.sait.peelin.dto.v1.auth.ChangePasswordRequest;
import com.sait.peelin.dto.v1.auth.DeactivateAccountRequest;
import com.sait.peelin.dto.v1.auth.LoginAccountChoice;
import com.sait.peelin.dto.v1.auth.LoginRequest;
import com.sait.peelin.dto.v1.auth.RegisterAvailabilityResponse;
import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.exception.AmbiguousLinkedLoginException;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.UserRepository;
import com.sait.peelin.support.GuestContactFiller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,50}$");
    /** Practical RFC 5322–oriented check aligned with mobile app validation. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final WelcomeEmailService welcomeEmailService;
    private final LinkedProfileSyncService linkedProfileSyncService;
    private final EmployeeCustomerLinkService employeeCustomerLinkService;
    private final UserLookupCacheService userLookupCacheService;
    private final CustomerLookupCacheService customerLookupCacheService;


    public AuthResponse login(LoginRequest request) {
        String rawPassword = request.getPassword();
        String explicitUsername = Optional.ofNullable(request.getUsername())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .orElse(null);
        String email = Optional.ofNullable(request.getEmail())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .orElse(null);

        if (explicitUsername != null) {
            User user = userRepository.findByUsername(explicitUsername)
                    .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), rawPassword));
            return buildAuthResponse(user, mintJwtForUser(user));
        }

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username or email required");
        }

        List<User> candidates = userRepository.findAllActiveByLoginPrincipal(email);
        List<User> passwordMatches = new ArrayList<>();
        for (User u : candidates) {
            if (StringUtils.hasText(u.getUserPasswordHash())
                    && passwordEncoder.matches(rawPassword, u.getUserPasswordHash())) {
                passwordMatches.add(u);
            }
        }

        if (passwordMatches.isEmpty()) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (passwordMatches.size() == 1) {
            User user = passwordMatches.getFirst();
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), rawPassword));
            return buildAuthResponse(user, mintJwtForUser(user));
        }

        if (passwordMatches.size() == 2
                && employeeCustomerLinkService.areLinkedUserIds(
                        passwordMatches.get(0).getUserId(), passwordMatches.get(1).getUserId())) {
            List<LoginAccountChoice> choices = passwordMatches.stream()
                    .map(u -> new LoginAccountChoice(
                            u.getUsername(),
                            u.getUserRole().name(),
                            roleChoiceLabel(u.getUserRole())))
                    .sorted(Comparator.comparing(LoginAccountChoice::role))
                    .toList();
            throw new AmbiguousLinkedLoginException(
                    "Your sign-in matches both your employee and customer accounts. Choose one to continue.",
                    choices);
        }

        throw new BadCredentialsException("Bad credentials");
    }

    private static String roleChoiceLabel(UserRole role) {
        if (role == null) {
            return "Account";
        }
        return switch (role) {
            case customer -> "Customer account";
            case employee -> "Employee account";
            case admin -> "Admin account";
        };
    }

    private String mintJwtForUser(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getUserPasswordHash())
                .authorities("ROLE_" + user.getUserRole().name().toUpperCase())
                .build();
        return jwtService.generateToken(userDetails);
    }

    /**
     * Lightweight check for multi-step UIs: username must be free; sign-in email must not be taken by another
     * <strong>customer</strong> or <strong>admin</strong> account. Employee sign-in rows may reuse the same address
     * so a new customer can register with that email for employee–customer linking.
     */
    @Transactional(readOnly = true)
    public RegisterAvailabilityResponse getRegisterAvailability(String username, String email) {
        String u = username != null ? username.trim() : "";
        String e = email != null ? email.trim().toLowerCase() : "";
        boolean usernameAvailable = u.isEmpty() || !userRepository.existsByUsernameIgnoreCase(u);
        boolean emailAvailable = e.isEmpty() || !isCustomerOrAdminSignInEmailTaken(e);
        boolean employeeLinkOffered = !e.isEmpty()
                && emailAvailable
                && employeeCustomerLinkService.findSingleUnlinkedEmployeeByWorkEmail(e).isPresent();
        return new RegisterAvailabilityResponse(usernameAvailable, emailAvailable, employeeLinkOffered);
    }

    private boolean isCustomerOrAdminSignInEmailTaken(String emailNorm) {
        return userRepository.existsByUserEmailIgnoreCaseAndUserRole(emailNorm, UserRole.customer.name())
                || userRepository.existsByUserEmailIgnoreCaseAndUserRole(emailNorm, UserRole.admin.name());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String usernameTrim = request.getUsername().trim();
        if (!StringUtils.hasText(usernameTrim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required");
        }
        if (userRepository.existsByUsernameIgnoreCase(usernameTrim)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        String emailNorm = request.getEmail().trim().toLowerCase();
        if (isCustomerOrAdminSignInEmailTaken(emailNorm)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        java.util.Optional<Employee> linkEmployee =
                employeeCustomerLinkService.findSingleUnlinkedEmployeeByWorkEmail(emailNorm);
        if (linkEmployee.isPresent()) {
            String elp = request.getEmployeeLinkPassword();
            if (!StringUtils.hasText(elp)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employee password required to link your customer account.");
            }
            User empUser = linkEmployee.get().getUser();
            if (empUser == null || !StringUtils.hasText(empUser.getUserPasswordHash())
                    || !passwordEncoder.matches(elp, empUser.getUserPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Employee password does not match this work email.");
            }
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
        user.setUsername(usernameTrim);
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

        welcomeEmailService.sendWelcomeEmail(user);

        customerRepository.findByUser_UserId(user.getUserId()).ifPresent(cust -> {
            boolean linked = employeeCustomerLinkService.tryAutoLinkForCustomer(cust);
            if (linked && employeeCustomerLinkService.isEligibleForEmployeeDiscount(cust.getId())) {
                res.setEmployeeDiscountLinkEstablished(true);
                res.setEmployeeDiscountLinkMessage(
                        "Your customer account is linked to your employee profile. You are eligible for the 20% employee discount on orders.");
            }
        });
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
        String oldUsername = u.getUsername();
        String oldEmail = u.getUserEmail();
        String targetUsername = oldUsername;
        String targetEmail = oldEmail;
        boolean dirty = false;

        if (req.getUsername() != null) {
            String nu = req.getUsername().trim();
            if (!nu.equals(u.getUsername())) {
                if (nu.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
                }
                if (!USERNAME_PATTERN.matcher(nu).matches()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
                }
                if (userRepository.existsByUsernameIgnoreCaseAndUserIdNot(nu, u.getUserId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
                }
                targetUsername = nu;
                dirty = true;
            }
        }

        if (req.getEmail() != null) {
            String ne = req.getEmail().trim();
            if (!ne.equals(u.getUserEmail())) {
                if (ne.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
                }
                if (ne.length() > 254 || !EMAIL_PATTERN.matcher(ne).matches()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
                }
                if (userRepository.existsOtherCustomerOrAdminWithEmailIgnoreCase(ne, u.getUserId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                }
                targetEmail = ne;
                dirty = true;
                Optional<Customer> cust = customerRepository.findByUser_UserId(u.getUserId());
                if (cust.isPresent()) {
                    Customer c = cust.get();
                    c.setCustomerEmail(ne);
                    customerRepository.save(c);
                    customerLookupCacheService.evictByUserId(u.getUserId());
                }
                Optional<Employee> emp = employeeRepository.findByUser_UserId(u.getUserId());
                if (emp.isPresent()) {
                    Employee e = emp.get();
                    e.setEmployeeWorkEmail(ne);
                    employeeRepository.save(e);
                }
                linkedProfileSyncService.afterLinkedUserSignInEmailChanged(u.getUserId(), ne);
            }
        }

        if (dirty) {
            int updated = userRepository.updateAccountIdentity(u.getUserId(), targetUsername, targetEmail);
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Account update failed");
            }
            customerService.evictCurrentCustomerCaches();
            if (oldUsername != null) {
                userLookupCacheService.evictByIdentifier(oldUsername);
            }
            if (oldEmail != null) {
                userLookupCacheService.evictByIdentifier(oldEmail);
            }
            userLookupCacheService.evictByIdentifier(targetUsername);
            userLookupCacheService.evictByIdentifier(targetEmail);
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(targetUsername)
                .password(u.getUserPasswordHash())
                .authorities("ROLE_" + u.getUserRole().name().toUpperCase())
                .build();
        String token = jwtService.generateToken(userDetails);
        AuthResponse res = buildAuthResponse(u, token);
        res.setUsername(targetUsername);
        res.setEmail(targetEmail);
        return res;
    }

    /**
     * Deactivates the current user after verifying their password. Any authenticated role may call this.
     */
    @Transactional
    public void deactivateAccount(DeactivateAccountRequest request) {
        User u = currentUserService.requireUser();
        if (!StringUtils.hasText(u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This account has no password set; contact support to deactivate.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        int updated = userRepository.updateActiveFlag(u.getUserId(), false);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Account deactivation failed");
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User u = currentUserService.requireUser();

        if (!StringUtils.hasText(u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This account has no password set. Use forgot password to create one first.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), u.getUserPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from your current password");
        }
        int updated = userRepository.updatePasswordHash(
                u.getUserId(), passwordEncoder.encode(request.getNewPassword()));
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Password update failed");
        }
        userLookupCacheService.evictByIdentifier(u.getUsername());
        userLookupCacheService.evictByIdentifier(u.getUserEmail());
    }

    public AuthResponse getUserInfoFromToken(String token) {
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow();

        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        res.setEmail(user.getUserEmail());
        populateProfileDisplayFields(res, user);
        return res;
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setUsername(user.getUsername());
        res.setRole(user.getUserRole().name());
        res.setUserId(user.getUserId());
        res.setEmail(user.getUserEmail());
        populateProfileDisplayFields(res, user);
        return res;
    }

    /**
     * Adds profile photo URL (from user) and, for employees, first/last name from the employee row.
     */
    private void populateProfileDisplayFields(AuthResponse res, User user) {
        res.setProfilePhotoPath(user.getProfilePhotoPath());
        if (user.getUserRole() == UserRole.employee) {
            employeeRepository.findByUser_UserId(user.getUserId()).ifPresent(emp -> {
                res.setFirstName(emp.getEmployeeFirstName());
                res.setLastName(emp.getEmployeeLastName());
            });
        }
    }
}
