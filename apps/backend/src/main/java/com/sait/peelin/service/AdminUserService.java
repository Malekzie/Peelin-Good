package com.sait.peelin.service;

import com.sait.peelin.dto.v1.UserSummaryDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserSummaryDto> list() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public UserSummaryDto setActive(UUID userId, boolean active) {
        User u = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
