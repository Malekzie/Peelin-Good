package com.sait.peelin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BatchInventoryId implements Serializable {
    private static final long serialVersionUID = -4416211946292073464L;
    @NotNull
    @Column(name = "batch_id", nullable = false)
    private Integer batchId;

    @NotNull
    @Column(name = "inventory_id", nullable = false)
    private Integer inventoryId;

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public Integer getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchInventoryId entity = (BatchInventoryId) o;
        return Objects.equals(this.batchId, entity.batchId) &&
                Objects.equals(this.inventoryId, entity.inventoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, inventoryId);
    }
}