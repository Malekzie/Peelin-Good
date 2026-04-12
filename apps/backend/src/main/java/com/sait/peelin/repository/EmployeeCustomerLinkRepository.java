package com.sait.peelin.repository;

import com.sait.peelin.model.EmployeeCustomerLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeCustomerLinkRepository extends JpaRepository<EmployeeCustomerLink, UUID> {

    @EntityGraph(attributePaths = {"employee", "employee.user", "customer", "customer.user"})
    Optional<EmployeeCustomerLink> findByCustomer_Id(UUID customerId);

    @EntityGraph(attributePaths = {"employee", "employee.user", "customer", "customer.user"})
    Optional<EmployeeCustomerLink> findByEmployee_Id(UUID employeeId);

    boolean existsByEmployee_Id(UUID employeeId);

    boolean existsByCustomer_Id(UUID customerId);

    @Query("""
            SELECT COUNT(l) FROM EmployeeCustomerLink l
            WHERE (l.customer.user.userId = :a AND l.employee.user.userId = :b)
               OR (l.customer.user.userId = :b AND l.employee.user.userId = :a)
            """)
    long countLinkBetweenUserIds(@Param("a") UUID a, @Param("b") UUID b);
}
