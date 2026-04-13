package com.sait.peelin.service;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.dto.v1.EmployeePatchRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeProfileService {

    private final EmployeeRepository employeeRepository;
    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;
    private final LinkedProfileSyncService linkedProfileSyncService;

    @Transactional(readOnly = true)
    @Cacheable(value = "employees", keyGenerator = "userIdKeyGenerator")
    public EmployeeDto me() {
        User u = currentUserService.requireUser();
        Employee e = employeeRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee profile"));
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public List<Integer> myBakeryIds() {
        return List.of(me().bakeryId());
    }

    @Transactional
    @CacheEvict(value = "employees", keyGenerator = "userIdKeyGenerator")
    public EmployeeDto patchMe(EmployeePatchRequest req) {
        User u = currentUserService.requireUser();
        Employee e = employeeRepository.findByUser_UserId(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee profile"));

        if (req.firstName() != null) e.setEmployeeFirstName(req.firstName());
        if (req.middleInitial() != null) {
            String v = req.middleInitial().trim();
            e.setEmployeeMiddleInitial(v.isEmpty() ? null : v);
        }
        if (req.lastName() != null) e.setEmployeeLastName(req.lastName());
        if (req.phone() != null) e.setEmployeePhone(req.phone());
        if (req.businessPhone() != null) e.setEmployeeBusinessPhone(req.businessPhone());
        if (req.workEmail() != null) e.setEmployeeWorkEmail(req.workEmail());
        if (req.addressId() != null) {
            e.setAddress(addressRepository.findById(req.addressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found")));
        }
        if (req.address() != null) {
            Address existing = e.getAddress();
            if (existing != null) {
                CatalogMapper.copyAddress(req.address(), existing);
                addressRepository.save(existing);
            } else {
                Address created = new Address();
                CatalogMapper.copyAddress(req.address(), created);
                e.setAddress(addressRepository.save(created));
            }
        }

        Employee saved = employeeRepository.save(e);
        linkedProfileSyncService.afterEmployeeProfilePatch(saved);
        return toDto(saved);
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "employees", key = "'all'")
    public List<EmployeeDto> listAll() {
        return employeeRepository.findAll().stream().map(this::toDto).toList();
    }
    @Transactional(readOnly = true)
    public EmployeeDto get(UUID id) {
        return toDto(employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
    }

    private EmployeeDto toDto(Employee e) {
        return new EmployeeDto(
                e.getId(),
                e.getUser().getUserId(),
                e.getBakery().getId(),
                e.getUser() != null ? e.getUser().getUsername() : null,
                e.getEmployeeFirstName(),
                e.getEmployeeMiddleInitial(),
                e.getEmployeeLastName(),
                e.getEmployeePosition(),
                e.getEmployeePhone(),
                e.getEmployeeBusinessPhone(),
                e.getEmployeeWorkEmail(),
                e.getAddress().getId(),
                CatalogMapper.address(e.getAddress()),
                e.getUser() != null ? e.getUser().getProfilePhotoPath() : null,
                e.getUser() != null && Boolean.TRUE.equals(e.getUser().getPhotoApprovalPending())
        );
    }
}
