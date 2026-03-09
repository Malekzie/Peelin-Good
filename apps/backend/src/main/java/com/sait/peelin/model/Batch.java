package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "batch", indexes = {
        @Index(name = "idx_batch_bakery",
                columnList = "bakery_id"),
        @Index(name = "idx_batch_product",
                columnList = "product_id")})
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id", nullable = false)
    private Bakery bakery;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @Column(name = "batch_production_date", nullable = false)
    private LocalDate batchProductionDate;

    @Column(name = "batch_expiry_date")
    private LocalDate batchExpiryDate;

    @NotNull
    @Column(name = "batch_quantity_produced", nullable = false)
    private Integer batchQuantityProduced;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Bakery getBakery() {
        return bakery;
    }

    public void setBakery(Bakery bakery) {
        this.bakery = bakery;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getBatchProductionDate() {
        return batchProductionDate;
    }

    public void setBatchProductionDate(LocalDate batchProductionDate) {
        this.batchProductionDate = batchProductionDate;
    }

    public LocalDate getBatchExpiryDate() {
        return batchExpiryDate;
    }

    public void setBatchExpiryDate(LocalDate batchExpiryDate) {
        this.batchExpiryDate = batchExpiryDate;
    }

    public Integer getBatchQuantityProduced() {
        return batchQuantityProduced;
    }

    public void setBatchQuantityProduced(Integer batchQuantityProduced) {
        this.batchQuantityProduced = batchQuantityProduced;
    }

}