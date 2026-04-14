package com.sait.peelin.service;

import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CustomerService customerService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LinkedProfileSyncService linkedProfileSyncService;

    @Mock
    private EmployeeCustomerLinkService employeeCustomerLinkService;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @Mock
    private UserLookupCacheService userLookupCacheService;

    @Mock
    private CustomerLookupCacheService customerLookupCacheService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldInitializeUserCorrectly() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        when(userRepository.existsByUsernameIgnoreCase("testuser")).thenReturn(false);
        when(userRepository.existsByUserEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(customerRepository.findGuestCustomersByEmailNormalized("test@example.com")).thenReturn(List.of());
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        Customer createdCustomer = mock(Customer.class);
        when(customerService.createRegisteredCustomer(any(User.class), any())).thenReturn(createdCustomer);
        when(customerRepository.findByUser_UserId(nullable(UUID.class))).thenReturn(Optional.of(createdCustomer));
        when(employeeCustomerLinkService.tryAutoLinkForCustomer(any(Customer.class))).thenReturn(false);

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
        verify(welcomeEmailService).sendWelcomeEmail(any(User.class));
    }
}
