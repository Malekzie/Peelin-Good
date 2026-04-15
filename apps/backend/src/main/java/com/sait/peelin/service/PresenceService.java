package com.sait.peelin.service;

import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which staff (employee/admin) users currently have a live WebSocket.
 * In-memory only; cleared on restart. That's acceptable — staff reconnect fast.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserRepository userRepository;

    private final Set<UUID> activeStaff = ConcurrentHashMap.newKeySet();

    public Set<UUID> activeStaffUserIds() {
        return Collections.unmodifiableSet(activeStaff);
    }

    public boolean isActive(UUID userId) {
        return userId != null && activeStaff.contains(userId);
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        // SessionConnectedEvent.getUser() can be null at STOMP CONNECTED time; wrap the raw
        // message to pull the authenticated principal from the header accessor instead.
        resolveStaffUserId(StompHeaderAccessor.wrap(event.getMessage()).getUser())
                .ifPresent(activeStaff::add);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // Session is already established by disconnect, so event.getUser() is reliable here.
        resolveStaffUserId(event.getUser()).ifPresent(activeStaff::remove);
    }

    private Optional<UUID> resolveStaffUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return Optional.empty();
        }
        // Principal.getName() is the username set by the auth filter on WebSocket handshake.
        return userRepository.findByUsername(principal.getName())
                .filter(u -> u.getUserRole() == UserRole.employee || u.getUserRole() == UserRole.admin)
                .map(User::getUserId);
    }
}
