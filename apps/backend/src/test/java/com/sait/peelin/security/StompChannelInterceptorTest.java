package com.sait.peelin.security;

import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.TokenDenylistService;
import com.sait.peelin.service.UserLookupCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock JwtService jwtService;
    @Mock TokenDenylistService tokenDenylistService;
    @Mock UserLookupCacheService userLookupCacheService;
    @Mock MessageChannel channel;

    @InjectMocks StompChannelInterceptor interceptor;

    private User staffUser;

    @BeforeEach
    void setUp() {
        staffUser = new User();
        staffUser.setUserId(UUID.randomUUID());
        staffUser.setUsername("staffmember");
        staffUser.setUserRole(UserRole.employee);
    }

    @Test
    void connect_WithValidJwtInHeader_SetsUserPrincipal() {
        String jwt = "valid.jwt.token";
        when(tokenDenylistService.isDenied(jwt)).thenReturn(false);
        when(jwtService.extractUsername(jwt)).thenReturn("staffmember");
        when(userLookupCacheService.findActiveByLoginIdentifier("staffmember")).thenReturn(staffUser);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + jwt);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("staffmember");
    }

    @Test
    void connect_WithMissingToken_ThrowsMessageDeliveryException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connect_WithDeniedToken_ThrowsMessageDeliveryException() {
        String jwt = "denied.token";
        when(tokenDenylistService.isDenied(jwt)).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + jwt);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void nonConnectFrame_PassesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/thread/1/typing");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }
}
