package com.sait.peelin.service;

import com.sait.peelin.dto.v1.UserSummaryDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

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
        return toDto(userRepository.save(u));
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
