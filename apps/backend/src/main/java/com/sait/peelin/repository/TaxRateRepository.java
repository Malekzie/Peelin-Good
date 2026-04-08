package com.sait.peelin.repository;

import com.sait.peelin.model.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, String> {
    Optional<TaxRate> findByProvinceNameIgnoreCase(String provinceName);
}
