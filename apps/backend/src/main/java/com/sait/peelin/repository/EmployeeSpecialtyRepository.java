package com.sait.peelin.repository;

import com.sait.peelin.model.EmployeeSpecialty;
import com.sait.peelin.model.EmployeeSpecialtyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeSpecialtyRepository
        extends JpaRepository<EmployeeSpecialty, EmployeeSpecialtyId> {

    List<EmployeeSpecialty> findByUserId(UUID userId);

    List<EmployeeSpecialty> findByCategory(String category);
}
