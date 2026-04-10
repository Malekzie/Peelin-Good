package com.sait.peelin.repository;

import com.sait.peelin.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByUser_UserId(UUID userId);

    List<Employee> findByBakery_Id(Integer bakeryId);

    @Query("SELECT e FROM Employee e JOIN FETCH e.user WHERE LOWER(TRIM(e.employeeWorkEmail)) = LOWER(TRIM(:email))")
    List<Employee> findByWorkEmailNormalized(@Param("email") String email);

    @Query(value = """
            SELECT e.* FROM employee e
            WHERE regexp_replace(btrim(e.employee_phone), '\\\\D', '', 'g') = :digits
            """, nativeQuery = true)
    List<Employee> findByPhoneDigits(@Param("digits") String digits);
}
