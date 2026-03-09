package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id", nullable = false)
    private Integer id;

    @Size(max = 120)
    @NotNull
    @Column(name = "address_line1", nullable = false, length = 120)
    private String addressLine1;

    @Size(max = 120)
    @Column(name = "address_line2", length = 120)
    private String addressLine2;

    @Size(max = 120)
    @NotNull
    @Column(name = "address_city", nullable = false, length = 120)
    private String addressCity;

    @Size(max = 80)
    @NotNull
    @Column(name = "address_province", nullable = false, length = 80)
    private String addressProvince;

    @Size(max = 10)
    @NotNull
    @Column(name = "address_postal_code", nullable = false, length = 10)
    private String addressPostalCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressProvince() {
        return addressProvince;
    }

    public void setAddressProvince(String addressProvince) {
        this.addressProvince = addressProvince;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

}