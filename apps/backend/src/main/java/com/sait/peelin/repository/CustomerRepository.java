package com.sait.peelin.repository;

import com.sait.peelin.model.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    Optional<Customer> findByUser_UserId(UUID userId);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("SELECT c FROM Customer c WHERE c.user IS NOT NULL AND c.user.photoApprovalPending = true")
    List<Customer> findByUserPhotoApprovalPendingTrue();

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    @Query("SELECT c FROM Customer c WHERE "
            + "LOWER(c.customerFirstName) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(c.customerLastName) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(c.customerEmail) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Customer> search(@Param("q") String q);

    @EntityGraph(attributePaths = {"address", "rewardTier", "user"})
    Optional<Customer> findById(UUID id);
}
