package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final TokenDenylistService tokenDenylistService;
    private final UserLookupCacheService userLookupCacheService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
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
