package com.sait.peelin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_rate")
public class TaxRate {
    @Id
    @Size(max = 80)
    @Column(name = "province_name", nullable = false, length = 80)
    private String provinceName;

    @NotNull
    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 3)
    private BigDecimal taxPercent;

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public BigDecimal getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(BigDecimal taxPercent) {
        this.taxPercent = taxPercent;
    }
}
