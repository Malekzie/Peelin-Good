package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Integer id;

    @Size(max = 120)
    @NotNull
    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Size(max = 1000)
    @Column(name = "product_description", length = 1000)
    private String productDescription;

    @NotNull
    @Column(name = "product_base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal productBasePrice;

    @Size(max = 500)
    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public BigDecimal getProductBasePrice() {
        return productBasePrice;
    }

    public void setProductBasePrice(BigDecimal productBasePrice) {
        this.productBasePrice = productBasePrice;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

}