package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @NotNull
    @Column(name = "order_item_quantity", nullable = false)
    private Integer orderItemQuantity;

    @NotNull
    @Column(name = "order_item_unit_price_at_time", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderItemUnitPriceAtTime;

    @NotNull
    @Column(name = "order_item_line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderItemLineTotal;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Integer getOrderItemQuantity() {
        return orderItemQuantity;
    }

    public void setOrderItemQuantity(Integer orderItemQuantity) {
        this.orderItemQuantity = orderItemQuantity;
    }

    public BigDecimal getOrderItemUnitPriceAtTime() {
        return orderItemUnitPriceAtTime;
    }

    public void setOrderItemUnitPriceAtTime(BigDecimal orderItemUnitPriceAtTime) {
        this.orderItemUnitPriceAtTime = orderItemUnitPriceAtTime;
    }

    public BigDecimal getOrderItemLineTotal() {
        return orderItemLineTotal;
    }

    public void setOrderItemLineTotal(BigDecimal orderItemLineTotal) {
        this.orderItemLineTotal = orderItemLineTotal;
    }

}