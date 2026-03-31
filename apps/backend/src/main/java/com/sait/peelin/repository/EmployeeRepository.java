package com.sait.peelin.repository;

import com.sait.peelin.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByUser_UserId(UUID userId);

    List<Employee> findByBakery_Id(Integer bakeryId);
}
