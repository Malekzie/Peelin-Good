package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_user",
                columnList = "user_id"),
        @Index(name = "idx_customer_guest_expiry",
                columnList = "guest_expiry_date")})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "uuid", nullable = false, unique = true, insertable = false, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_tier_id", nullable = false)
    private RewardTier rewardTier;

    @Size(max = 50)
    @NotNull
    @Column(name = "customer_first_name", nullable = false, length = 50)
    private String customerFirstName;

    @Size(max = 2)
    @Column(name = "customer_middle_initial", length = 2)
    private String customerMiddleInitial;

    @Size(max = 50)
    @NotNull
    @Column(name = "customer_last_name", nullable = false, length = 50)
    private String customerLastName;

    @Size(max = 20)
    @NotNull
    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Size(max = 20)
    @Column(name = "customer_business_phone", length = 20)
    private String customerBusinessPhone;

    @Size(max = 254)
    @NotNull
    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "customer_reward_balance", nullable = false)
    private Integer customerRewardBalance;

    @Column(name = "customer_tier_assigned_date")
    private LocalDate customerTierAssignedDate;

    @Column(name = "guest_expiry_date")
    private LocalDate guestExpiryDate;

    @Size(max = 500)
    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "photo_approval_pending", nullable = false)
    private Boolean photoApprovalPending;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public RewardTier getRewardTier() {
        return rewardTier;
    }

    public void setRewardTier(RewardTier rewardTier) {
        this.rewardTier = rewardTier;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerMiddleInitial() {
        return customerMiddleInitial;
    }

    public void setCustomerMiddleInitial(String customerMiddleInitial) {
        this.customerMiddleInitial = customerMiddleInitial;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerBusinessPhone() {
        return customerBusinessPhone;
    }

    public void setCustomerBusinessPhone(String customerBusinessPhone) {
        this.customerBusinessPhone = customerBusinessPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public Integer getCustomerRewardBalance() {
        return customerRewardBalance;
    }

    public void setCustomerRewardBalance(Integer customerRewardBalance) {
        this.customerRewardBalance = customerRewardBalance;
    }

    public LocalDate getCustomerTierAssignedDate() {
        return customerTierAssignedDate;
    }

    public void setCustomerTierAssignedDate(LocalDate customerTierAssignedDate) {
        this.customerTierAssignedDate = customerTierAssignedDate;
    }

    public LocalDate getGuestExpiryDate() {
        return guestExpiryDate;
    }

    public void setGuestExpiryDate(LocalDate guestExpiryDate) {
        this.guestExpiryDate = guestExpiryDate;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public Boolean getPhotoApprovalPending() {
        return photoApprovalPending;
    }

    public void setPhotoApprovalPending(Boolean photoApprovalPending) {
        this.photoApprovalPending = photoApprovalPending;
    }

}