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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("TODO: add CustomerLookupCacheService mock + align exception mappings after service refactors.")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private RewardTierRepository rewardTierRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfilePhotoStorageService profilePhotoStorageService;
    @Mock private CurrentUserService currentUserService;
    @Mock private RewardTierService rewardTierService;
    @Mock private EmployeeCustomerLinkService employeeCustomerLinkService;
    @Mock private LinkedProfileSyncService linkedProfileSyncService;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void stubEmployeeLinking() {
        lenient().when(employeeCustomerLinkService.isEligibleForEmployeeDiscount(any())).thenReturn(false);
        lenient().when(employeeCustomerLinkService.tryAutoLinkForCustomer(any())).thenReturn(false);
    }

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
        when(rewardTierService.tierForBalance(anyInt())).thenReturn(Optional.of(tier));
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(customerRepository.findGuestCustomersByEmailNormalized("jamie@example.com"))
                .thenReturn(List.of(guestCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            if (a.getId() == null) {
                a.setId(999);
            }
            return a;
        });
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
        existingCustomer.setCustomerPhone("4035550000");
        existingCustomer.setCustomerEmail("jamie@example.com");
        existingCustomer.setUser(null);

        GuestCustomerRequest request = new GuestCustomerRequest();
        request.setFirstName("Someone");
        request.setLastName("Else");
        request.setPhone("4035559999");
        request.setEmail("jamie@example.com");
        request.setAddressLine1("555 New St");
        request.setCity("Toronto");
        request.setProvince("Ontario");
        request.setPostalCode("M5V2T6");

        when(userRepository.existsByUserEmailIgnoreCase("jamie@example.com")).thenReturn(false);
        when(customerRepository.findByCustomerEmailNormalized("jamie@example.com"))
                .thenReturn(List.of(existingCustomer));
        when(customerRepository.findGuestCustomersByEmailNormalized("jamie@example.com"))
                .thenReturn(List.of(existingCustomer));
        when(customerRepository.save(existingCustomer)).thenReturn(existingCustomer);

        Customer result = customerService.resolveOrCreateGuestCustomer(request);

        assertEquals(existingCustomer, result);
    }

    @Test
    void resolveOrCreateGuestCustomer_RejectsWhenEmailMatchesUserAccount() {
        GuestCustomerRequest request = new GuestCustomerRequest();
        request.setEmail("member@example.com");
        request.setPhone("4035559999");

        when(userRepository.existsByUserEmailIgnoreCase("member@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> customerService.resolveOrCreateGuestCustomer(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void resolveOrCreateGuestCustomer_RejectsWhenEmailOnRegisteredCustomer() {
        User u = new User();
        u.setUserId(UUID.randomUUID());

        Customer registered = new Customer();
        registered.setUser(u);
        registered.setCustomerEmail("acct@example.com");

        GuestCustomerRequest request = new GuestCustomerRequest();
        request.setEmail("acct@example.com");
        request.setPhone("4035551111");

        when(userRepository.existsByUserEmailIgnoreCase("acct@example.com")).thenReturn(false);
        when(customerRepository.findByCustomerEmailNormalized("acct@example.com"))
                .thenReturn(List.of(registered));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> customerService.resolveOrCreateGuestCustomer(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createRegisteredCustomer_LinksExistingGuestByEmail() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserEmail("reuse@email.com");

        RewardTier tier = new RewardTier();
        tier.setId(1);

        Customer guest = new Customer();
        guest.setId(UUID.randomUUID());
        guest.setRewardTier(tier);
        guest.setCustomerEmail("reuse@email.com");
        guest.setCustomerPhone("4035551111");
        guest.setCustomerRewardBalance(42);
        guest.setGuestExpiryDate(LocalDate.now().plusMonths(6));
        guest.setUser(null);

        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(customerRepository.findGuestCustomersByEmailNormalized("reuse@email.com"))
                .thenReturn(List.of(guest));
        when(customerRepository.save(guest)).thenReturn(guest);

        Customer result = customerService.createRegisteredCustomer(user, null);

        assertEquals(guest, result);
        assertEquals(user, guest.getUser());
        assertNull(guest.getGuestExpiryDate());
        assertEquals("reuse@email.com", guest.getCustomerEmail());
        assertEquals("4035551111", guest.getCustomerPhone());
        assertEquals(42, guest.getCustomerRewardBalance());
        verify(customerRepository).save(guest);
    }

    @Test
    void createRegisteredCustomer_LinksExistingGuestByPhoneWhenEmailSynthetic() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserEmail("newreal@email.com");

        RewardTier tier = new RewardTier();
        tier.setId(1);

        Customer guest = new Customer();
        guest.setId(UUID.randomUUID());
        guest.setRewardTier(tier);
        guest.setCustomerEmail("guest-synthetic@local");
        guest.setCustomerPhone("+1 (403) 555-2222");
        guest.setCustomerRewardBalance(10);
        guest.setUser(null);

        UUID guestId = guest.getId();
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(customerRepository.findGuestCustomersByEmailNormalized("newreal@email.com"))
                .thenReturn(List.of());
        when(customerRepository.findGuestCustomerIdsByPhoneDigits("14035552222"))
                .thenReturn(List.of(guestId));
        when(customerRepository.findByIdIn(List.of(guestId))).thenReturn(List.of(guest));
        when(customerRepository.save(guest)).thenReturn(guest);

        Customer result = customerService.createRegisteredCustomer(user, "+1 (403) 555-2222");

        assertEquals(guest, result);
        assertEquals(user, guest.getUser());
        assertEquals("newreal@email.com", guest.getCustomerEmail());
        assertEquals("(403) 555-2222", guest.getCustomerPhone());
        verify(customerRepository).save(guest);
    }
}
