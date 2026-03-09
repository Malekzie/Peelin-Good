package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id", nullable = false)
    private Bakery bakery;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Size(max = 120)
    @NotNull
    @Column(name = "inventory_item_name", nullable = false, length = 120)
    private String inventoryItemName;

    @Size(max = 40)
    @NotNull
    @Column(name = "inventory_item_type", nullable = false, length = 40)
    private String inventoryItemType;

    @NotNull
    @Column(name = "inventory_quantity_on_hand", nullable = false, precision = 12, scale = 3)
    private BigDecimal inventoryQuantityOnHand;

    @Size(max = 20)
    @NotNull
    @Column(name = "inventory_unit_of_measure", nullable = false, length = 20)
    private String inventoryUnitOfMeasure;

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

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getInventoryItemName() {
        return inventoryItemName;
    }

    public void setInventoryItemName(String inventoryItemName) {
        this.inventoryItemName = inventoryItemName;
    }

    public String getInventoryItemType() {
        return inventoryItemType;
    }

    public void setInventoryItemType(String inventoryItemType) {
        this.inventoryItemType = inventoryItemType;
    }

    public BigDecimal getInventoryQuantityOnHand() {
        return inventoryQuantityOnHand;
    }

    public void setInventoryQuantityOnHand(BigDecimal inventoryQuantityOnHand) {
        this.inventoryQuantityOnHand = inventoryQuantityOnHand;
    }

    public String getInventoryUnitOfMeasure() {
        return inventoryUnitOfMeasure;
    }

    public void setInventoryUnitOfMeasure(String inventoryUnitOfMeasure) {
        this.inventoryUnitOfMeasure = inventoryUnitOfMeasure;
    }

}