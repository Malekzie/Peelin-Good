package com.sait.peelin.service;

import com.sait.peelin.dto.v1.AddressUpsertRequest;
import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.CustomerPatchRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardTierRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProfilePhotoStorageService profilePhotoStorageService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CustomerDto> listAdmin(String search) {
        currentUserService.requireUser();
        if (StringUtils.hasText(search)) {
            String q = search.trim();
            return customerRepository.search(q).stream().map(this::toDto).toList();
        }
        return customerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> pendingPhotos() {
        return customerRepository.findByUserPhotoApprovalPendingTrue().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto get(UUID id) {
        return toDto(customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
    }

    @Transactional(readOnly = true)
    public CustomerDto me() {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));
        return toDto(c);
    }

    @Transactional
    public CustomerDto patchMe(CustomerPatchRequest req) {
        User u = currentUserService.requireUser();
        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));
        applyPatch(c, req);
        return toDto(customerRepository.save(c));
    }

    @Transactional
    public CustomerDto patch(UUID id, CustomerPatchRequest req) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        applyPatch(c, req);
        return toDto(customerRepository.save(c));
    }

    @Transactional
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
        }
    }

    @Transactional
    public void rejectPhoto(UUID id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (c.getUser() != null) {
            User freshUser = userRepository.findById(c.getUser().getUserId()).orElse(c.getUser());
            String existingPhotoPath = freshUser.getProfilePhotoPath();
            int updated = userRepository.updateProfilePhotoState(freshUser.getUserId(), null, false);
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reject photo");
            }
            profilePhotoStorageService.deleteCustomerProfilePhoto(existingPhotoPath);
        }
    }

    @Transactional
    public CustomerDto uploadMyProfilePhoto(MultipartFile photo) {
        User u = currentUserService.requireUser();
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

        Customer c = customerRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer profile"));

        String previousPhotoPath = u.getProfilePhotoPath();
        String uploadedUrl = profilePhotoStorageService.uploadCustomerProfilePhoto(u.getUserId(), photo);
        int updated = userRepository.updateProfilePhotoState(u.getUserId(), uploadedUrl, true);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist profile photo");
        }
        // Keep the current persistence-context object in sync for this request's DTO mapping.
        u.setProfilePhotoPath(uploadedUrl);
        u.setPhotoApprovalPending(true);
        if (previousPhotoPath != null && !previousPhotoPath.isBlank() && !previousPhotoPath.equals(uploadedUrl)) {
            profilePhotoStorageService.deleteCustomerProfilePhoto(previousPhotoPath);
        }
        return toDto(c);
    }

    private void applyPatch(Customer c, CustomerPatchRequest req) {
        if (req.getRewardBalance() != null) c.setCustomerRewardBalance(req.getRewardBalance());
        if (req.getFirstName() != null) c.setCustomerFirstName(req.getFirstName());
        if (req.getMiddleInitial() != null) {
            String v = req.getMiddleInitial().trim();
            c.setCustomerMiddleInitial(v.isEmpty() ? null : v);
        }
        if (req.getLastName() != null) c.setCustomerLastName(req.getLastName());
        if (req.getPhone() != null) c.setCustomerPhone(req.getPhone());
        if (req.getBusinessPhone() != null) c.setCustomerBusinessPhone(req.getBusinessPhone());
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
        }
        if (req.getPhotoApprovalPending() != null && c.getUser() != null) {
            c.getUser().setPhotoApprovalPending(req.getPhotoApprovalPending());
            userRepository.save(c.getUser());
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

    private CustomerDto toDto(Customer c) {
        Address addr = c.getAddress();
        User dtoUser = null;
        if (c.getUser() != null && c.getUser().getUserId() != null) {
            dtoUser = userRepository.findById(c.getUser().getUserId()).orElse(c.getUser());
        }
        return new CustomerDto(
                c.getId(),
                dtoUser != null ? dtoUser.getUserId() : null,
                dtoUser != null ? dtoUser.getUsername() : null,
                c.getRewardTier().getId(),
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
                dtoUser != null && Boolean.TRUE.equals(dtoUser.getPhotoApprovalPending())
        );
    }
}
