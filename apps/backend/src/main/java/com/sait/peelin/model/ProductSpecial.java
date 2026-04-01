package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_special")
public class ProductSpecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_special_id", nullable = false)
    private Integer productSpecialId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    /**
     * Calendar day for this featured row (PostgreSQL column name is {@code "date"}).
     */
    @NotNull
    @Column(name = "\"date\"", nullable = false)
    private LocalDate featuredOn;

    @NotNull
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    public Integer getProductSpecialId() {
        return productSpecialId;
    }

    public void setProductSpecialId(Integer productSpecialId) {
        this.productSpecialId = productSpecialId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public LocalDate getFeaturedOn() {
        return featuredOn;
    }

    public void setFeaturedOn(LocalDate featuredOn) {
        this.featuredOn = featuredOn;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent != null ? discountPercent : BigDecimal.ZERO;
    }
}
