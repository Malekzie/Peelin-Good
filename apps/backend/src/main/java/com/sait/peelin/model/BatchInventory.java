package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "batch_inventory")
public class BatchInventory {
    @EmbeddedId
    private BatchInventoryId id;

    @MapsId("batchId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @MapsId("inventoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @NotNull
    @Column(name = "quantity_used", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityUsed;

    @Size(max = 20)
    @NotNull
    @Column(name = "unit_of_measure_at_time", nullable = false, length = 20)
    private String unitOfMeasureAtTime;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "usage_recorded_date", nullable = false)
    private OffsetDateTime usageRecordedDate;

    public BatchInventoryId getId() {
        return id;
    }

    public void setId(BatchInventoryId id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(BigDecimal quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public String getUnitOfMeasureAtTime() {
        return unitOfMeasureAtTime;
    }

    public void setUnitOfMeasureAtTime(String unitOfMeasureAtTime) {
        this.unitOfMeasureAtTime = unitOfMeasureAtTime;
    }

    public OffsetDateTime getUsageRecordedDate() {
        return usageRecordedDate;
    }

    public void setUsageRecordedDate(OffsetDateTime usageRecordedDate) {
        this.usageRecordedDate = usageRecordedDate;
    }

}