package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardTierRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RewardTierRepository rewardTierRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldInitializeUserCorrectly() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setFirstName("First");
        request.setLastName("Last");
        request.setPhone("1234567890");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByUserEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(rewardTierRepository.findFirstByOrderByRewardTierMinPointsAsc()).thenReturn(Optional.of(new com.sait.peelin.model.RewardTier()));

        // Act
        authService.register(request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getUserEmail());
        assertEquals("encoded_password", savedUser.getUserPasswordHash());
        assertEquals(UserRole.customer, savedUser.getUserRole());
        assertTrue(savedUser.getActive(), "User should be active by default");
        assertFalse(savedUser.getPhotoApprovalPending(), "Photo approval should not be pending by default");
        assertNotNull(savedUser.getUserCreatedAt());
    }
}
