package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.service.ChatLookupCacheService;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final UserLookupCacheService userLookupCacheService;
    private final ChatLookupCacheService chatLookupCacheService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.matches("/topic/chat/thread/\\d+/.*")) {
                Object principal = accessor.getUser();
                if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
                    Object details = authToken.getDetails();
                    if (details instanceof User userDetails &&
                            userDetails.getUserRole() == UserRole.customer) {
                        String[] parts = destination.split("/");
                        // destination: /topic/chat/thread/{id}/messages -> parts[4] = id
                        try {
                            int threadId = Integer.parseInt(parts[4]);
                            Integer allowedThreadId = chatLookupCacheService
                                    .findOpenThreadIdForCustomer(userDetails.getUserId());
                            if (allowedThreadId == null || !allowedThreadId.equals(threadId)) {
                                throw new MessageDeliveryException("Forbidden: cannot subscribe to this thread");
                            }
                        } catch (NumberFormatException ignored) {
                            // malformed path — block it
                            throw new MessageDeliveryException("Forbidden");
                        }
                    }
                }
            }
            // Staff-only thread list feed — customers must not see other customers' threads.
            if ("/topic/chat/threads".equals(destination)) {
                Object principal = accessor.getUser();
                if (principal instanceof UsernamePasswordAuthenticationToken authToken
                        && authToken.getDetails() instanceof User userDetails
                        && userDetails.getUserRole() == UserRole.customer) {
                    throw new MessageDeliveryException("Forbidden");
                }
            }
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            java.util.Map<String, Object> attrs = accessor.getSessionAttributes();
            log.info("WS CONNECT session attrs: {}", attrs != null ? attrs.keySet() : "null");
            log.info("WS CONNECT token resolved: {}", resolveToken(accessor) != null ? "YES" : "NULL");
            String jwt = resolveToken(accessor);
            if (jwt == null || tokenDenylistService.isDenied(jwt)) {
                throw new MessageDeliveryException("Unauthorized");
            }
            try {
                String username = jwtService.extractUsername(jwt);
                User user = userLookupCacheService.findActiveByLoginIdentifier(username);
                if (user == null) {
                    throw new MessageDeliveryException("Unauthorized");
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getUserRole().name().toUpperCase()))
                );
                auth.setDetails(user);
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (MessageDeliveryException e) {
                throw e;
            } catch (Exception e) {
                throw new MessageDeliveryException("Unauthorized");
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        // Web clients: token cookie copied to session attrs by HandshakeInterceptor
        java.util.Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            Object tokenAttr = sessionAttrs.get("token");
            if (tokenAttr instanceof String tokenStr && !tokenStr.isBlank()) {
                return tokenStr;
            }
        }
        // Mobile/desktop clients: Bearer token in STOMP Authorization header
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Legacy: cookie passed explicitly as STOMP native header (mobile fallback)
        String cookieHeader = accessor.getFirstNativeHeader("cookie");
        if (cookieHeader != null) {
            for (String part : cookieHeader.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("token=")) {
                    return trimmed.substring(6);
                }
            }
        }
        return null;
    }
}
