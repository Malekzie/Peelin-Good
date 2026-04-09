package com.sait.peelin.repository;

import com.sait.peelin.model.CustomerPreference;
import com.sait.peelin.model.CustomerPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, CustomerPreferenceId> {
    @Query("SELECT DISTINCT p FROM CustomerPreference p JOIN FETCH p.tag WHERE p.customer.id = :customerId")
    List<CustomerPreference> findByCustomer_IdWithTag(@Param("customerId") UUID customerId);

    List<CustomerPreference> findByCustomer_Id(UUID customerId);
    void deleteByCustomer_Id(UUID customerId);
}
