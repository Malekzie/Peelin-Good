package com.sait.peelin.service;

import com.sait.peelin.dto.v1.AddressUpsertRequest;
import com.sait.peelin.dto.v1.CustomerBootstrapRequest;
import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.CustomerPatchRequest;
import com.sait.peelin.dto.v1.GuestCustomerRequest;
import com.sait.peelin.dto.v1.ProfilePhotoResponse;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.CustomerPreferenceRepository;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardTierRepository;
import com.sait.peelin.repository.UserRepository;
import com.sait.peelin.support.GuestContactFiller;
import com.sait.peelin.support.PhoneNumberFormatter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final RewardTierService rewardTierService;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProfilePhotoStorageService profilePhotoStorageService;
    private final CurrentUserService currentUserService;
    private final CustomerLookupCacheService customerLookupCacheService;
    private final UserLookupCacheService userLookupCacheService;
    private final CustomerPreferenceRepository customerPreferenceRepository;
    private final CacheManager cacheManager;
    private final EmployeeCustomerLinkService employeeCustomerLinkService;
    private final LinkedProfileSyncService linkedProfileSyncService;
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'all:' + #search")
    public List<CustomerDto> listAdmin(String search) {
        currentUserService.requireUser();
        if (StringUtils.hasText(search)) {
            String q = search.trim();
            return customerRepository.search(q).stream().map(this::toDto).toList();
        }
        return customerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'pending-photos'")
    public List<CustomerDto> pendingPhotos() {
        return customerRepository.findByUserPhotoApprovalPendingTrue().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto get(UUID id) {
        return toDto(customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customers", keyGenerator = "userIdKeyGenerator")
    public CustomerDto me() {
        User u = currentUserService.requireUser();
        Customer c = customerLookupCacheService.findByUserId(u.getUserId());
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile");
        }
        return toDto(c);
    }

    @Transactional
    public Customer createRegisteredCustomer(User user, String phone) {
        Customer existing = customerRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        Customer reusableGuest = findSingleGuestByContact(user.getUserEmail(), phone);
        if (reusableGuest != null) {
            reusableGuest.setUser(user);
            reusableGuest.setGuestExpiryDate(null);
            reusableGuest.setCustomerEmail(user.getUserEmail());
            if (StringUtils.hasText(phone)) {
                reusableGuest.setCustomerPhone(PhoneNumberFormatter.formatStoredPhone(phone));
            }
            if (reusableGuest.getCustomerTierAssignedDate() == null) {
                reusableGuest.setCustomerTierAssignedDate(LocalDate.now());
            }
            Customer saved = customerRepository.save(reusableGuest);
            customerLookupCacheService.evictByUserId(user.getUserId());
            return saved;
        }

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setRewardTier(lowestRewardTier());
        customer.setCustomerEmail(user.getUserEmail());
        customer.setCustomerPhone(StringUtils.hasText(phone)
                ? PhoneNumberFormatter.formatStoredPhone(phone)
                : GuestContactFiller.allocateSyntheticPhoneDigits());
        customer.setCustomerRewardBalance(0);
        customer.setCustomerTierAssignedDate(LocalDate.now());
        customer.setGuestExpiryDate(null);

        Customer saved = customerRepository.save(customer);
        customerLookupCacheService.evictByUserId(user.getUserId());
        return saved;
    }

    @Transactional
    public CustomerDto createMyProfile(CustomerBootstrapRequest request) {
        User u = currentUserService.requireUser();
        if (customerRepository.findByUser_UserId(u.getUserId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer profile already exists");
        }
        Customer reusableGuest = findSingleGuestByContact(u.getUserEmail(), request.getPhone());
        if (reusableGuest == null) {
            reusableGuest = findExactGuestCustomer(
                    request.getFirstName(),
                    request.getMiddleInitial(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getBusinessPhone(),
                    u.getUserEmail(),
                    request.getAddressLine1(),
                    request.getAddressLine2(),
                    request.getCity(),
                    request.getProvince(),
                    request.getPostalCode()
            );
        }
        if (reusableGuest != null) {
            reusableGuest.setUser(u);
            reusableGuest.setGuestExpiryDate(null);
            applyCustomerIdentity(
                    reusableGuest,
                    request.getFirstName(),
                    request.getMiddleInitial(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getBusinessPhone(),
                    u.getUserEmail()
            );
            Address linkedAddress = createAddress(
                    request.getAddressLine1(),
                    request.getAddressLine2(),
                    request.getCity(),
                    request.getProvince(),
                    request.getPostalCode()
            );
            reusableGuest.setAddress(linkedAddress);
            customerRepository.save(reusableGuest);
            customerLookupCacheService.evictByUserId(u.getUserId());
            return toDto(reusableGuest);
        }

        RewardTier lowestTier = lowestRewardTier();
        Address address = createAddress(
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getCity(),
                request.getProvince(),
                request.getPostalCode()
        );

        Customer customer = new Customer();
        customer.setUser(u);
        customer.setRewardTier(lowestTier);
        customer.setAddress(address);
        applyCustomerIdentity(
                customer,
                request.getFirstName(),
                request.getMiddleInitial(),
                request.getLastName(),
                request.getPhone(),
                request.getBusinessPhone(),
                u.getUserEmail()
        );
        customer.setCustomerRewardBalance(0);
        customer.setGuestExpiryDate(null);
        customerRepository.save(customer);
        customerLookupCacheService.evictByUserId(u.getUserId());
        return toDto(customer);
    }

    @Transactional
    public Customer resolveOrCreateGuestCustomer(GuestCustomerRequest request) {
        assertGuestCheckoutEmailNotRegisteredAccount(request.getEmail());

        Customer existing = findSingleGuestByContact(request.getEmail(), request.getPhone());
        if (existing == null && hasFullGuestIdentity(request)) {
            existing = findExactGuestCustomer(
                    request.getFirstName(),
                    request.getMiddleInitial(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getBusinessPhone(),
                    request.getEmail(),
                    request.getAddressLine1(),
                    request.getAddressLine2(),
                    request.getCity(),
                    request.getProvince(),
                    request.getPostalCode()
            );
        }
        if (existing != null) {
            mergeSupplementalGuestProfile(existing, request);
            return customerRepository.save(existing);
        }

        Customer customer = new Customer();
        customer.setRewardTier(lowestRewardTier());
        customer.setAddress(createAddressIfPresent(request));
        applyNewGuestContact(customer, request);
        applyGuestOptionalNames(customer, request);
        customer.setCustomerBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(request.getBusinessPhone()));
        customer.setCustomerRewardBalance(0);
        customer.setGuestExpiryDate(LocalDate.now().plusYears(1));
        return customerRepository.save(customer);
    }

    @Transactional
    @CacheEvict(value = "customers", keyGenerator = "userIdKeyGenerator")
    public CustomerDto patchMe(CustomerPatchRequest req) {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));
        applyPatch(c, req);
        Customer saved = customerRepository.save(c);
        linkedProfileSyncService.afterCustomerProfilePatch(saved);
        customerLookupCacheService.evictByUserId(u.getUserId());
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDto patch(UUID id, CustomerPatchRequest req) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        applyPatch(c, req);
        Customer saved = customerRepository.save(c);
        if (saved.getUser() != null && saved.getUser().getUserId() != null) {
            customerLookupCacheService.evictByUserId(saved.getUser().getUserId());
        }
        linkedProfileSyncService.afterCustomerProfilePatch(saved);
        return toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void approvePhoto(UUID id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (c.getUser() != null) {
            User freshUser = userRepository.findById(c.getUser().getUserId()).orElse(c.getUser());
            int updated = userRepository.updateProfilePhotoState(
                    freshUser.getUserId(),
                    freshUser.getProfilePhotoPath(),
                    false
            );
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to approve photo");
            }
            customerLookupCacheService.evictByUserId(freshUser.getUserId());
        }
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void rejectPhoto(UUID id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (c.getUser() != null) {
            User freshUser = userRepository.findById(c.getUser().getUserId()).orElse(c.getUser());
            String existingPhotoPath = freshUser.getProfilePhotoPath();
            int updated = userRepository.updateProfilePhotoState(freshUser.getUserId(), null, false);
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reject photo");
            }
            customerLookupCacheService.evictByUserId(freshUser.getUserId());
            profilePhotoStorageService.deleteCustomerProfilePhoto(existingPhotoPath);
        }
    }

    @Transactional
    public ProfilePhotoResponse uploadMyProfilePhoto(MultipartFile photo) {
        User u = currentUserService.requireUser();
        validateProfilePhotoFile(photo);

        String previousPhotoPath = u.getProfilePhotoPath();
        String uploadedUrl = profilePhotoStorageService.uploadCustomerProfilePhoto(u.getUserId(), photo);
        int updated = userRepository.updateProfilePhotoState(u.getUserId(), uploadedUrl, true);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist profile photo");
        }
        customerLookupCacheService.evictByUserId(u.getUserId());
        u.setProfilePhotoPath(uploadedUrl);
        u.setPhotoApprovalPending(true);
        if (previousPhotoPath != null && !previousPhotoPath.isBlank() && !previousPhotoPath.equals(uploadedUrl)) {
            profilePhotoStorageService.deleteCustomerProfilePhoto(previousPhotoPath);
        }
        return new ProfilePhotoResponse(uploadedUrl, true);
    }

    private static void validateProfilePhotoFile(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is required");
        }
        String contentType = photo.getContentType() != null ? photo.getContentType().toLowerCase() : "";
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg") && !contentType.equals("image/png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG and PNG images are allowed");
        }
        if (photo.getSize() > 5L * 1024L * 1024L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo exceeds 5MB limit");
        }
    }

    private void applyPatch(Customer c, CustomerPatchRequest req) {
        if (req.getRewardBalance() != null) c.setCustomerRewardBalance(req.getRewardBalance());
        if (req.getFirstName() != null) c.setCustomerFirstName(req.getFirstName());
        if (req.getMiddleInitial() != null) {
            String v = req.getMiddleInitial().trim();
            c.setCustomerMiddleInitial(v.isEmpty() ? null : v);
        }
        if (req.getLastName() != null) c.setCustomerLastName(req.getLastName());
        if (req.getPhone() != null) c.setCustomerPhone(PhoneNumberFormatter.formatStoredPhone(req.getPhone()));
        if (req.getBusinessPhone() != null) {
            c.setCustomerBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(req.getBusinessPhone()));
        }
        if (req.getEmail() != null) c.setCustomerEmail(req.getEmail());
        if (req.getAddressId() != null) {
            c.setAddress(addressRepository.findById(req.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found")));
        }
        if (req.getAddress() != null) {
            upsertCustomerAddress(c, req.getAddress());
        }
        if (req.getRewardTierId() != null) {
            RewardTier rt = rewardTierRepository.findById(req.getRewardTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reward tier not found"));
            c.setRewardTier(rt);
        } else if (req.getRewardBalance() != null) {
            int b = c.getCustomerRewardBalance() != null ? c.getCustomerRewardBalance() : 0;
            rewardTierService.tierForBalance(b).ifPresent(c::setRewardTier);
        }
        if (req.getPhotoApprovalPending() != null && c.getUser() != null) {
            c.getUser().setPhotoApprovalPending(req.getPhotoApprovalPending());
            User savedUser = userRepository.save(c.getUser());
            userLookupCacheService.evictByIdentifier(savedUser.getUsername());
            userLookupCacheService.evictByIdentifier(savedUser.getUserEmail());
        }
        if (req.getUsername() != null && c.getUser() != null) {
            String oldUsername = c.getUser().getUsername();
            c.getUser().setUsername(req.getUsername());
            User savedUser = userRepository.save(c.getUser());
            if (oldUsername != null) {
                userLookupCacheService.evictByIdentifier(oldUsername);
            }
            userLookupCacheService.evictByIdentifier(savedUser.getUsername());
            userLookupCacheService.evictByIdentifier(savedUser.getUserEmail());
        }
    }

    private void upsertCustomerAddress(Customer c, AddressUpsertRequest req) {
        Address existing = c.getAddress();
        if (existing != null) {
            CatalogMapper.copyAddress(req, existing);
            addressRepository.save(existing);
        } else {
            Address created = new Address();
            CatalogMapper.copyAddress(req, created);
            Address saved = addressRepository.save(created);
            c.setAddress(saved);
        }
    }

    private RewardTier lowestRewardTier() {
        return rewardTierRepository.findFirstByOrderByRewardTierMinPointsAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No reward tiers configured"));
    }

    private Address createAddress(String line1, String line2, String city, String province, String postalCode) {
        Address address = new Address();
        address.setAddressLine1(normalizeRequired(line1));
        address.setAddressLine2(normalizeOptional(line2));
        address.setAddressCity(normalizeRequired(city));
        address.setAddressProvince(normalizeRequired(province));
        address.setAddressPostalCode(normalizeRequired(postalCode));
        return addressRepository.save(address);
    }

    private Address createAddressIfPresent(GuestCustomerRequest req) {
        if (!StringUtils.hasText(req.getAddressLine1())) {
            return null;
        }
        return createAddress(
                req.getAddressLine1(),
                req.getAddressLine2(),
                req.getCity(),
                req.getProvince(),
                req.getPostalCode()
        );
    }

    private boolean hasFullGuestIdentity(GuestCustomerRequest r) {
        return StringUtils.hasText(r.getFirstName())
                && StringUtils.hasText(r.getLastName())
                && StringUtils.hasText(r.getAddressLine1())
                && StringUtils.hasText(r.getCity())
                && StringUtils.hasText(r.getProvince())
                && StringUtils.hasText(r.getPostalCode())
                && StringUtils.hasText(r.getPhone())
                && StringUtils.hasText(r.getEmail());
    }

    /**
     * Guest checkout may not use an email tied to a registered account. Pure guest rows
     * (customer without user) are allowed.
     */
    private void assertGuestCheckoutEmailNotRegisteredAccount(String emailRaw) {
        if (!StringUtils.hasText(emailRaw)) {
            return;
        }
        String trimmed = emailRaw.trim();
        if (GuestContactFiller.isSyntheticGuestEmail(trimmed)) {
            return;
        }
        String emailNorm = trimmed.toLowerCase();
        if (userRepository.existsByUserEmailIgnoreCase(emailNorm)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This email is already registered. Sign in to complete your order.");
        }
        for (Customer c : customerRepository.findByCustomerEmailNormalized(emailNorm)) {
            if (c.getUser() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This email is already registered. Sign in to complete your order.");
            }
        }
    }

    private Customer findSingleGuestByContact(String email, String phoneRaw) {
        if (StringUtils.hasText(email)) {
            List<Customer> byEmail = customerRepository.findGuestCustomersByEmailNormalized(email.trim());
            if (byEmail.size() == 1) {
                return byEmail.get(0);
            }
        }
        String digits = GuestContactFiller.normalizeDigits(phoneRaw);
        if (digits.length() >= 10) {
            List<UUID> ids = customerRepository.findGuestCustomerIdsByPhoneDigits(digits);
            List<Customer> loaded = ids.isEmpty() ? List.of() : customerRepository.findByIdIn(ids);
            if (loaded.size() == 1) {
                return loaded.get(0);
            }
        }
        return null;
    }

    private void applyNewGuestContact(Customer customer, GuestCustomerRequest req) {
        String emailIn = normalizeOptional(req.getEmail());
        String digits = GuestContactFiller.normalizeDigits(req.getPhone());
        boolean hasEmail = StringUtils.hasText(emailIn);
        boolean hasPhone = digits.length() >= 10;
        final String storedEmail;
        final String storedPhone;
        if (hasEmail && hasPhone) {
            storedEmail = emailIn.toLowerCase();
            storedPhone = normalizeRequired(req.getPhone()).trim();
        } else if (hasEmail) {
            storedEmail = emailIn.toLowerCase();
            storedPhone = ensureUniqueSyntheticPhone();
        } else {
            storedEmail = GuestContactFiller.syntheticEmailForPhoneDigits(digits);
            storedPhone = normalizeRequired(req.getPhone()).trim();
        }
        customer.setCustomerEmail(storedEmail);
        customer.setCustomerPhone(storedPhone);
    }

    private String ensureUniqueSyntheticPhone() {
        for (int i = 0; i < 24; i++) {
            String candidate = GuestContactFiller.allocateSyntheticPhoneDigits();
            if (customerRepository.countCustomersWithPhoneDigits(candidate) == 0) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate guest phone placeholder");
    }

    private void applyGuestOptionalNames(Customer customer, GuestCustomerRequest req) {
        customer.setCustomerFirstName(normalizeEmptyToNull(req.getFirstName()));
        customer.setCustomerMiddleInitial(normalizeOptional(req.getMiddleInitial()));
        customer.setCustomerLastName(normalizeEmptyToNull(req.getLastName()));
    }

    private String normalizeEmptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void mergeSupplementalGuestProfile(Customer existing, GuestCustomerRequest req) {
        if (StringUtils.hasText(req.getEmail()) && GuestContactFiller.isSyntheticGuestEmail(existing.getCustomerEmail())) {
            existing.setCustomerEmail(req.getEmail().trim().toLowerCase());
        }
        if (existing.getCustomerFirstName() == null && StringUtils.hasText(req.getFirstName())) {
            existing.setCustomerFirstName(req.getFirstName().trim());
        }
        if (existing.getCustomerLastName() == null && StringUtils.hasText(req.getLastName())) {
            existing.setCustomerLastName(req.getLastName().trim());
        }
        if (existing.getCustomerMiddleInitial() == null && StringUtils.hasText(req.getMiddleInitial())) {
            existing.setCustomerMiddleInitial(normalizeOptional(req.getMiddleInitial()));
        }
        if (StringUtils.hasText(req.getBusinessPhone())) {
            existing.setCustomerBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(req.getBusinessPhone()));
        }
        if (existing.getAddress() == null && StringUtils.hasText(req.getAddressLine1())) {
            existing.setAddress(createAddress(
                    req.getAddressLine1(),
                    req.getAddressLine2(),
                    req.getCity(),
                    req.getProvince(),
                    req.getPostalCode()
            ));
        }
    }

    private void applyCustomerIdentity(Customer customer,
                                       String firstName,
                                       String middleInitial,
                                       String lastName,
                                       String phone,
                                       String businessPhone,
                                       String email) {
        customer.setCustomerFirstName(normalizeRequired(firstName));
        customer.setCustomerMiddleInitial(normalizeOptional(middleInitial));
        customer.setCustomerLastName(normalizeRequired(lastName));
        customer.setCustomerPhone(PhoneNumberFormatter.formatStoredPhone(phone));
        customer.setCustomerBusinessPhone(PhoneNumberFormatter.formatStoredPhoneOrNull(businessPhone));
        customer.setCustomerEmail(normalizeRequired(email).toLowerCase());
    }

    private Customer findExactGuestCustomer(String firstName,
                                            String middleInitial,
                                            String lastName,
                                            String phone,
                                            String businessPhone,
                                            String email,
                                            String addressLine1,
                                            String addressLine2,
                                            String city,
                                            String province,
                                            String postalCode) {
        List<Customer> matches = customerRepository.findExactGuestMatches(
                normalizeRequired(firstName),
                normalizeOptional(middleInitial),
                normalizeRequired(lastName),
                normalizeRequired(phone),
                normalizeOptional(businessPhone),
                normalizeRequired(email).toLowerCase(),
                normalizeRequired(addressLine1),
                normalizeOptional(addressLine2),
                normalizeRequired(city),
                normalizeRequired(province),
                normalizeRequired(postalCode)
        );
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private CustomerDto toDto(Customer c) {
        Address addr = c.getAddress();
        User dtoUser = c.getUser();
        int points = c.getCustomerRewardBalance() != null ? c.getCustomerRewardBalance() : 0;
        RewardTier tier = rewardTierService.tierForBalance(points).orElse(c.getRewardTier());
        return new CustomerDto(
                c.getId(),
                dtoUser != null ? dtoUser.getUserId() : null,
                dtoUser != null ? dtoUser.getUsername() : null,
                tier != null ? tier.getId() : null,
                tier != null ? tier.getRewardTierName() : null,
                tier != null ? tier.getRewardTierDiscountRate() : null,
                c.getCustomerFirstName(),
                c.getCustomerMiddleInitial(),
                c.getCustomerLastName(),
                c.getCustomerPhone(),
                c.getCustomerBusinessPhone(),
                c.getCustomerEmail(),
                c.getCustomerRewardBalance(),
                addr != null ? addr.getId() : null,
                CatalogMapper.address(addr),
                dtoUser != null ? dtoUser.getProfilePhotoPath() : null,
                dtoUser != null && Boolean.TRUE.equals(dtoUser.getPhotoApprovalPending()),
                employeeCustomerLinkService.isEligibleForEmployeeDiscount(c.getId())
        );
    }

    @Transactional
    public void deleteMe() {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));

        customerLookupCacheService.evictByUserId(u.getUserId());
        customerRepository.delete(c);
        userRepository.delete(u);
    }

}
