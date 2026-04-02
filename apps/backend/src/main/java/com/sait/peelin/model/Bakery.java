package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @ColumnDefault("'open'")
    @Column(name = "status", nullable = false, columnDefinition = "bakery_status")
    private BakeryStatus status;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Size(max = 2048)
    @Column(name = "bakery_image_url", length = 2048)
    private String bakeryImageUrl;

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

    public BakeryStatus getStatus() {
        return status;
    }

    public void setStatus(BakeryStatus status) {
        this.status = status;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getBakeryImageUrl() {
        return bakeryImageUrl;
    }

    public void setBakeryImageUrl(String bakeryImageUrl) {
        this.bakeryImageUrl = bakeryImageUrl;
    }

}