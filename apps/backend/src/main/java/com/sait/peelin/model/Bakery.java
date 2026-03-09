package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "bakery")
public class Bakery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bakery_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Size(max = 100)
    @NotNull
    @Column(name = "bakery_name", nullable = false, length = 100)
    private String bakeryName;

    @Size(max = 20)
    @NotNull
    @Column(name = "bakery_phone", nullable = false, length = 20)
    private String bakeryPhone;

    @Size(max = 254)
    @NotNull
    @Column(name = "bakery_email", nullable = false, length = 254)
    private String bakeryEmail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getBakeryName() {
        return bakeryName;
    }

    public void setBakeryName(String bakeryName) {
        this.bakeryName = bakeryName;
    }

    public String getBakeryPhone() {
        return bakeryPhone;
    }

    public void setBakeryPhone(String bakeryPhone) {
        this.bakeryPhone = bakeryPhone;
    }

    public String getBakeryEmail() {
        return bakeryEmail;
    }

    public void setBakeryEmail(String bakeryEmail) {
        this.bakeryEmail = bakeryEmail;
    }

}