package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review", indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_customer", columnList = "customer_id")})
public class Review {
    @Id
    @GeneratedValue
    @Column(name = "review_id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @NotNull
    @Column(name = "review_rating", nullable = false)
    private Short reviewRating;

    @Size(max = 2000)
    @Column(name = "review_comment", length = 2000)
    private String reviewComment;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "review_submitted_date", nullable = false)
    private OffsetDateTime reviewSubmittedDate;

    @NotNull
    @ColumnDefault("'pending'")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "review_status", nullable = false, columnDefinition = "review_status")
    private ReviewStatus reviewStatus;

    @Column(name = "review_approval_date")
    private OffsetDateTime reviewApprovalDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public Short getReviewRating() {
        return reviewRating;
    }

    public void setReviewRating(Short reviewRating) {
        this.reviewRating = reviewRating;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public OffsetDateTime getReviewSubmittedDate() {
        return reviewSubmittedDate;
    }

    public void setReviewSubmittedDate(OffsetDateTime reviewSubmittedDate) {
        this.reviewSubmittedDate = reviewSubmittedDate;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public OffsetDateTime getReviewApprovalDate() {
        return reviewApprovalDate;
    }

    public void setReviewApprovalDate(OffsetDateTime reviewApprovalDate) {
        this.reviewApprovalDate = reviewApprovalDate;
    }

}