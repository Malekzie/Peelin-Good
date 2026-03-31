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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RewardTierRepository rewardTierRepository;
    private final AddressRepository addressRepository;
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
        return customerRepository.findByPhotoApprovalPendingTrue().stream().map(this::toDto).toList();
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
        c.setPhotoApprovalPending(false);
        customerRepository.save(c);
    }

    @Transactional
    public void rejectPhoto(UUID id) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        c.setPhotoApprovalPending(false);
        c.setProfilePhotoPath(null);
        customerRepository.save(c);
    }

    private void applyPatch(Customer c, CustomerPatchRequest req) {
        if (req.getRewardBalance() != null) c.setCustomerRewardBalance(req.getRewardBalance());
        if (req.getFirstName() != null) c.setCustomerFirstName(req.getFirstName());
        if (req.getMiddleInitial() != null) c.setCustomerMiddleInitial(req.getMiddleInitial());
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
        if (req.getProfilePhotoPath() != null) c.setProfilePhotoPath(req.getProfilePhotoPath());
        if (req.getPhotoApprovalPending() != null) c.setPhotoApprovalPending(req.getPhotoApprovalPending());
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
        return new CustomerDto(
                c.getId(),
                c.getUser() != null ? c.getUser().getUserId() : null,
                c.getRewardTier().getId(),
                c.getCustomerFirstName(),
                c.getCustomerMiddleInitial(),
                c.getCustomerLastName(),
                c.getCustomerPhone(),
                c.getCustomerEmail(),
                c.getCustomerRewardBalance(),
                addr != null ? addr.getId() : null,
                CatalogMapper.address(addr),
                c.getProfilePhotoPath(),
                Boolean.TRUE.equals(c.getPhotoApprovalPending())
        );
    }
}
