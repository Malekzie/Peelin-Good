package com.sait.peelin.service;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.dto.v1.EmployeeUpsertRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Bakery;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.BakeryRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BakeryRepository bakeryRepository;
    private final EmployeeProfileService employeeProfileService;

    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDto create(EmployeeUpsertRequest req) {
        if (employeeRepository.findByUser_UserId(req.userId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has an employee profile");
        }
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        requireAdminOrEmployeeUser(user);
        Address address = addressRepository.findById(req.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        Bakery bakery = bakeryRepository.findById(req.bakeryId())
                .orElseThrow(() -> new ResourceNotFoundException("Bakery not found"));

        Employee e = new Employee();
        e.setUser(user);
        e.setAddress(address);
        e.setBakery(bakery);
        e.setEmployeeFirstName(req.firstName());
        e.setEmployeeMiddleInitial(req.middleInitial());
        e.setEmployeeLastName(req.lastName());
        e.setEmployeePosition(req.position());
        e.setEmployeePhone(req.phone());
        e.setEmployeeBusinessPhone(req.businessPhone());
        e.setEmployeeWorkEmail(req.workEmail());
        user.setPhotoApprovalPending(false);
        userRepository.save(user);
        employeeRepository.save(e);
        return employeeProfileService.get(e.getId());
    }

    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeDto update(UUID id, EmployeeUpsertRequest req) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        requireAdminOrEmployeeUser(user);
        Address address = addressRepository.findById(req.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        Bakery bakery = bakeryRepository.findById(req.bakeryId())
                .orElseThrow(() -> new ResourceNotFoundException("Bakery not found"));

        employeeRepository.findByUser_UserId(req.userId()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already linked to another employee");
            }
        });

        e.setUser(user);
        e.setAddress(address);
        e.setBakery(bakery);
        e.setEmployeeFirstName(req.firstName());
        e.setEmployeeMiddleInitial(req.middleInitial());
        e.setEmployeeLastName(req.lastName());
        e.setEmployeePosition(req.position());
        e.setEmployeePhone(req.phone());
        e.setEmployeeBusinessPhone(req.businessPhone());
        e.setEmployeeWorkEmail(req.workEmail());
        employeeRepository.save(e);
        return employeeProfileService.get(id);
    }

    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    /** Admins are staff; employee rows may be linked to either an admin or an employee login. */
    private static void requireAdminOrEmployeeUser(User user) {
        UserRole r = user.getUserRole();
        if (r != UserRole.admin && r != UserRole.employee) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee profile must be linked to a user with role admin or employee");
        }
    }
}
