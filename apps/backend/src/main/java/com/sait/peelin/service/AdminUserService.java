package com.sait.peelin.service;

import com.sait.peelin.dto.v1.UserCreateRequest;
import com.sait.peelin.dto.v1.UserSummaryDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;
    private final UserLookupCacheService userLookupCacheService;
    private final PasswordEncoder passwordEncoder;

    public List<UserSummaryDto> list() {
        User actor = currentUserService.requireUser();
        if (actor.getUserRole() == UserRole.admin) {
            return userRepository.findAll().stream()
                    .filter(u -> u.getUserRole() == UserRole.employee || u.getUserRole() == UserRole.customer)
                    .map(this::toDto)
                    .toList();
        }
        if (actor.getUserRole() == UserRole.employee) {
            return userRepository.findAll().stream()
                    .filter(u -> u.getUserRole() == UserRole.customer)
                    .map(this::toDto)
                    .toList();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    /**
     * Returns all admin and employee users — used for messaging recipient lists.
     * Does not include customers.
     */
    public List<UserSummaryDto> listStaff() {
        currentUserService.requireUser();
        return userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == UserRole.admin || u.getUserRole() == UserRole.employee)
                .map(this::toDto)
                .toList();
    }

    /**
     * User ids that already have an employee or customer profile (for admin UI pickers).
     * Lightweight — does not load full customer/employee rows.
     */
    public List<String> listProfileLinkedUserIds() {
        currentUserService.requireUser();
        Set<String> ids = new LinkedHashSet<>();
        for (UUID id : employeeRepository.findDistinctLinkedUserIds()) {
            if (id != null) {
                ids.add(id.toString());
            }
        }
        for (UUID id : customerRepository.findDistinctLinkedUserIds()) {
            if (id != null) {
                ids.add(id.toString());
            }
        }
        return new ArrayList<>(ids);
    }

    @Transactional
    public UserSummaryDto setActive(UUID userId, boolean active) {
        User actor = currentUserService.requireUser();
        User u = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (actor.getUserId().equals(u.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change your own account");
        }
        if (actor.getUserRole() == UserRole.employee) {
            if (u.getUserRole() != UserRole.customer) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employees can only manage customers");
            }
        } else if (actor.getUserRole() == UserRole.admin) {
            if (u.getUserRole() == UserRole.admin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot manage other admins");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        u.setActive(active);
        User saved = userRepository.save(u);
        userLookupCacheService.evictByIdentifier(saved.getUsername());
        userLookupCacheService.evictByIdentifier(saved.getUserEmail());
        return toDto(saved);
    }

    @Transactional
    public UserSummaryDto createUser(UserCreateRequest req) {
        User actor = currentUserService.requireUser();
        if (actor.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }

        String usernameTrimmed = req.getUsername().trim();
        String emailNorm = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(usernameTrimmed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByUserEmail(emailNorm)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserRole role = "employee".equals(req.getRole()) ? UserRole.employee : UserRole.customer;

        User user = new User();
        user.setUsername(usernameTrimmed);
        user.setUserEmail(emailNorm);
        user.setUserPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setUserRole(role);
        user.setUserCreatedAt(OffsetDateTime.now());
        user.setActive(true);
        user.setPhotoApprovalPending(false);
        User saved = userRepository.save(user);

        // Customer-role accounts get their profile from self-registration, OAuth, or admin "New Customer"
        // in the desktop app — not an empty stub row here.

        return toDto(saved);
    }

    private UserSummaryDto toDto(User u) {
        return new UserSummaryDto(
                u.getUserId(),
                u.getUsername(),
                u.getUserEmail(),
                u.getUserRole(),
                Boolean.TRUE.equals(u.getActive())
        );
    }
}
