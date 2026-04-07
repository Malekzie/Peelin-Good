package com.sait.peelin.service;

import com.sait.peelin.dto.v1.CustomerBootstrapRequest;
import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.GuestCustomerRequest;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardTierRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private RewardTierRepository rewardTierRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfilePhotoStorageService profilePhotoStorageService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createMyProfile_ReusesGuestCustomerByEmail() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserEmail("jamie@example.com");
        user.setUsername("jamie");

        RewardTier tier = new RewardTier();
        tier.setId(1);

        Address address = new Address();
        address.setId(10);
        address.setAddressLine1("123 Main St");
        address.setAddressCity("Calgary");
        address.setAddressProvince("Alberta");
        address.setAddressPostalCode("T2T2T2");

        Customer guestCustomer = new Customer();
        guestCustomer.setId(UUID.randomUUID());
        guestCustomer.setRewardTier(tier);
        guestCustomer.setAddress(address);
        guestCustomer.setCustomerFirstName("Jamie");
        guestCustomer.setCustomerLastName("Guest");
        guestCustomer.setCustomerPhone("(403) 555-1212");
        guestCustomer.setCustomerEmail("jamie@example.com");
        guestCustomer.setCustomerRewardBalance(0);
        guestCustomer.setGuestExpiryDate(LocalDate.now().plusDays(30));

        CustomerBootstrapRequest request = new CustomerBootstrapRequest();
        request.setFirstName("Different");
        request.setLastName("Person");
        request.setPhone("(587) 555-8888");
        request.setAddressLine1("999 Other Ave");
        request.setCity("Edmonton");
        request.setProvince("Alberta");
        request.setPostalCode("T5J0A1");

        when(currentUserService.requireUser()).thenReturn(user);
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerEmailNormalized("jamie@example.com"))
                .thenReturn(List.of(guestCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        CustomerDto result = customerService.createMyProfile(request);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.userId());
        assertEquals(guestCustomer.getId(), result.id());
        assertEquals("jamie@example.com", result.email());
        assertEquals(user, guestCustomer.getUser());
        assertNull(guestCustomer.getGuestExpiryDate());
        verify(customerRepository).save(guestCustomer);
    }

    @Test
    void resolveOrCreateGuestCustomer_ReusesByEmailBeforeExactMatch() {
        RewardTier tier = new RewardTier();
        tier.setId(1);

        Customer existingCustomer = new Customer();
        existingCustomer.setId(UUID.randomUUID());
        existingCustomer.setRewardTier(tier);
        existingCustomer.setCustomerRewardBalance(250);
        existingCustomer.setCustomerEmail("jamie@example.com");

        GuestCustomerRequest request = new GuestCustomerRequest();
        request.setFirstName("Someone");
        request.setLastName("Else");
        request.setPhone("4035559999");
        request.setEmail("jamie@example.com");
        request.setAddressLine1("555 New St");
        request.setCity("Toronto");
        request.setProvince("Ontario");
        request.setPostalCode("M5V2T6");

        when(customerRepository.findByCustomerEmailNormalized("jamie@example.com"))
                .thenReturn(List.of(existingCustomer));

        Customer result = customerService.resolveOrCreateGuestCustomer(request);

        assertEquals(existingCustomer, result);
    }
}
