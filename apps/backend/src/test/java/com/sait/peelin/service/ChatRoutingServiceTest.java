package com.sait.peelin.service;

import com.sait.peelin.model.EmployeeSpecialty;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.EmployeeSpecialtyRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatRoutingServiceTest {

    private EmployeeSpecialtyRepository specialtyRepo;
    private ChatThreadRepository threadRepo;
    private UserRepository userRepo;
    private PresenceService presence;
    private ChatRoutingService router;

    @BeforeEach
    void setUp() {
        specialtyRepo = mock(EmployeeSpecialtyRepository.class);
        threadRepo = mock(ChatThreadRepository.class);
        userRepo = mock(UserRepository.class);
        presence = mock(PresenceService.class);
        router = new ChatRoutingService(specialtyRepo, threadRepo, userRepo, presence);
    }

    private User staff(String idHex) {
        User u = new User();
        u.setUserId(UUID.fromString("00000000-0000-0000-0000-" + idHex));
        u.setUserRole(UserRole.employee);
        return u;
    }

    @Test
    void picks_specialist_when_exactly_one_matches() {
        User alice = staff("000000000001");
        User bob = staff("000000000002");
        when(presence.activeStaffUserIds()).thenReturn(Set.of(alice.getUserId(), bob.getUserId()));
        when(specialtyRepo.findByCategory("order_issue"))
                .thenReturn(List.of(new EmployeeSpecialty(alice.getUserId(), "order_issue")));
        when(userRepo.findAllById(any())).thenReturn(List.of(alice));
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(alice.getUserId(), "open")).thenReturn(3L);

        Optional<User> picked = router.pickStaff("order_issue");

        assertThat(picked).isPresent();
        assertThat(picked.get().getUserId()).isEqualTo(alice.getUserId());
    }

    @Test
    void picks_least_busy_specialist_when_multiple_match() {
        User alice = staff("000000000001");
        User bob = staff("000000000002");
        when(presence.activeStaffUserIds()).thenReturn(Set.of(alice.getUserId(), bob.getUserId()));
        when(specialtyRepo.findByCategory("account_help")).thenReturn(List.of(
                new EmployeeSpecialty(alice.getUserId(), "account_help"),
                new EmployeeSpecialty(bob.getUserId(), "account_help")
        ));
        when(userRepo.findAllById(any())).thenReturn(List.of(alice, bob));
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(alice.getUserId(), "open")).thenReturn(3L);
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(bob.getUserId(), "open")).thenReturn(1L);

        Optional<User> picked = router.pickStaff("account_help");

        assertThat(picked).isPresent();
        assertThat(picked.get().getUserId()).isEqualTo(bob.getUserId());
    }

    @Test
    void falls_back_to_any_active_staff_when_no_specialist() {
        User alice = staff("000000000001");
        User bob = staff("000000000002");
        when(presence.activeStaffUserIds()).thenReturn(Set.of(alice.getUserId(), bob.getUserId()));
        when(specialtyRepo.findByCategory("feedback")).thenReturn(List.of());
        when(userRepo.findAllById(any())).thenReturn(List.of(alice, bob));
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(alice.getUserId(), "open")).thenReturn(5L);
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(bob.getUserId(), "open")).thenReturn(2L);

        Optional<User> picked = router.pickStaff("feedback");

        assertThat(picked).isPresent();
        assertThat(picked.get().getUserId()).isEqualTo(bob.getUserId());
    }

    @Test
    void returns_empty_when_no_staff_online() {
        when(presence.activeStaffUserIds()).thenReturn(Set.of());

        Optional<User> picked = router.pickStaff("general");

        assertThat(picked).isEmpty();
    }

    @Test
    void tie_break_is_deterministic_by_uuid_order() {
        User alice = staff("000000000001");
        User bob = staff("000000000002");
        when(presence.activeStaffUserIds()).thenReturn(Set.of(alice.getUserId(), bob.getUserId()));
        when(specialtyRepo.findByCategory("general")).thenReturn(List.of(
                new EmployeeSpecialty(alice.getUserId(), "general"),
                new EmployeeSpecialty(bob.getUserId(), "general")
        ));
        when(userRepo.findAllById(any())).thenReturn(List.of(alice, bob));
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(alice.getUserId(), "open")).thenReturn(2L);
        when(threadRepo.countByEmployeeUser_UserIdAndStatus(bob.getUserId(), "open")).thenReturn(2L);

        Optional<User> picked = router.pickStaff("general");

        assertThat(picked).isPresent();
        assertThat(picked.get().getUserId()).isEqualTo(alice.getUserId()); // smaller UUID wins
    }
}
