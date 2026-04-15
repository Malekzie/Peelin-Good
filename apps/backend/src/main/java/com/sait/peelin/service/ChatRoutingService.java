package com.sait.peelin.service;

import com.sait.peelin.model.EmployeeSpecialty;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.EmployeeSpecialtyRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoutingService {

    private final EmployeeSpecialtyRepository specialtyRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    /**
     * Pick a staff user to auto-assign a new thread to.
     * 1. Active staff whose specialties include `category`.
     * 2. If empty, any active staff.
     * 3. If still empty, return empty.
     * Within the chosen pool: least-busy (fewest OPEN threads), ties broken by UUID.
     */
    @Transactional(readOnly = true)
    public Optional<User> pickStaff(String category) {
        Set<UUID> activeIds = presenceService.activeStaffUserIds();
        if (activeIds.isEmpty()) {
            return Optional.empty();
        }

        List<UUID> specialistIds = specialtyRepository.findByCategory(category).stream()
                .map(EmployeeSpecialty::getUserId)
                .filter(activeIds::contains)
                .toList();

        List<User> pool = specialistIds.isEmpty()
                ? userRepository.findAllById(activeIds)
                : userRepository.findAllById(specialistIds);

        if (pool.isEmpty()) {
            return Optional.empty();
        }

        return pool.stream().min(
                Comparator
                        .comparingLong((User u) -> chatThreadRepository
                                .countByEmployeeUser_UserIdAndStatus(u.getUserId(), "open"))
                        .thenComparing(User::getUserId)
        );
    }
}
