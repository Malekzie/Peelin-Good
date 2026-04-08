package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"order\"", indexes = {
        @Index(name = "idx_order_number",
                columnList = "order_number"),
        @Index(name = "idx_order_customer",
                columnList = "customer_id"),
        @Index(name = "idx_order_bakery",
                columnList = "bakery_id"),
        @Index(name = "idx_order_status",
                columnList = "order_status")}, uniqueConstraints = {@UniqueConstraint(name = "order_order_number_key",
        columnNames = {"order_number"})})
public class Order {
    @Id
    @GeneratedValue
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID id;

    @Size(max = 20)
    @NotNull
    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id", nullable = false)
    private Bakery bakery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @Size(max = 100)
    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Size(max = 254)
    @Column(name = "guest_email", length = 254)
    private String guestEmail;

    @Size(max = 20)
    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "order_placed_datetime", nullable = false)
    private OffsetDateTime orderPlacedDatetime;

    @Column(name = "order_scheduled_datetime")
    private OffsetDateTime orderScheduledDatetime;

    @Column(name = "order_delivered_datetime")
    private OffsetDateTime orderDeliveredDatetime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "order_method", nullable = false, columnDefinition = "order_method")
    private OrderMethod orderMethod;

    @Size(max = 500)
    @Column(name = "order_comment", length = 500)
    private String orderComment;

    @NotNull
    @Column(name = "order_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderTotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "order_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderDiscount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "order_tax_rate", nullable = false, precision = 5, scale = 3)
    private BigDecimal orderTaxRate;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "order_tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderTaxAmount;

    @NotNull
    @ColumnDefault("'pending_payment'")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "order_status", nullable = false, columnDefinition = "order_status")
    private OrderStatus orderStatus;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Bakery getBakery() {
        return bakery;
    }

    public void setBakery(Bakery bakery) {
        this.bakery = bakery;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public OffsetDateTime getOrderPlacedDatetime() {
        return orderPlacedDatetime;
    }

    public void setOrderPlacedDatetime(OffsetDateTime orderPlacedDatetime) {
        this.orderPlacedDatetime = orderPlacedDatetime;
    }

    public OffsetDateTime getOrderScheduledDatetime() {
        return orderScheduledDatetime;
    }

    public void setOrderScheduledDatetime(OffsetDateTime orderScheduledDatetime) {
        this.orderScheduledDatetime = orderScheduledDatetime;
    }

    public OffsetDateTime getOrderDeliveredDatetime() {
        return orderDeliveredDatetime;
    }

    public void setOrderDeliveredDatetime(OffsetDateTime orderDeliveredDatetime) {
        this.orderDeliveredDatetime = orderDeliveredDatetime;
    }

    public OrderMethod getOrderMethod() {
        return orderMethod;
    }

    public void setOrderMethod(OrderMethod orderMethod) {
        this.orderMethod = orderMethod;
    }

    public String getOrderComment() {
        return orderComment;
    }

    public void setOrderComment(String orderComment) {
        this.orderComment = orderComment;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public BigDecimal getOrderDiscount() {
        return orderDiscount;
    }

    public void setOrderDiscount(BigDecimal orderDiscount) {
        this.orderDiscount = orderDiscount;
    }

    public BigDecimal getOrderTaxRate() {
        return orderTaxRate;
    }

    public void setOrderTaxRate(BigDecimal orderTaxRate) {
        this.orderTaxRate = orderTaxRate;
    }

    public BigDecimal getOrderTaxAmount() {
        return orderTaxAmount;
    }

    public void setOrderTaxAmount(BigDecimal orderTaxAmount) {
        this.orderTaxAmount = orderTaxAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

}