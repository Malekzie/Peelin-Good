package com.sait.peelin.service;

import com.sait.peelin.dto.v1.EmployeeDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Employee;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
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
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
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

    public List<EmployeeDto> listAll() {
        return employeeRepository.findAll().stream().map(this::toDto).toList();
    }

    public EmployeeDto get(UUID id) {
        return toDto(employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
    }

    private EmployeeDto toDto(Employee e) {
        return new EmployeeDto(
                e.getId(),
                e.getUser().getUserId(),
                e.getBakery().getId(),
                e.getEmployeeFirstName(),
                e.getEmployeeMiddleInitial(),
                e.getEmployeeLastName(),
                e.getEmployeePosition(),
                e.getEmployeePhone(),
                e.getEmployeeWorkEmail(),
                e.getAddress().getId(),
                e.getUser() != null ? e.getUser().getProfilePhotoPath() : null,
                e.getUser() != null && Boolean.TRUE.equals(e.getUser().getPhotoApprovalPending())
        );
    }
}
