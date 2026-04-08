package com.sait.peelin.repository;

import com.sait.peelin.model.CustomerPreference;
import com.sait.peelin.model.CustomerPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, CustomerPreferenceId> {
    List<CustomerPreference> findByCustomer_Id(UUID customerId);
    void deleteByCustomer_Id(UUID customerId);
}
