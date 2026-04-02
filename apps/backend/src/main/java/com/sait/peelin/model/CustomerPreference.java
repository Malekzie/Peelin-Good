package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_preference")
public class CustomerPreference {
    @EmbeddedId
    private CustomerPreferenceId id;

    @MapsId("customerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "preference_type", nullable = false, columnDefinition = "preference_type")
    private PreferenceType preferenceType;

    @Column(name = "preference_strength")
    private Short preferenceStrength;

    public CustomerPreferenceId getId() {
        return id;
    }

    public void setId(CustomerPreferenceId id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public PreferenceType getPreferenceType() {
        return preferenceType;
    }

    public void setPreferenceType(PreferenceType preferenceType) {
        this.preferenceType = preferenceType;
    }

    public Short getPreferenceStrength() {
        return preferenceStrength;
    }

    public void setPreferenceStrength(Short preferenceStrength) {
        this.preferenceStrength = preferenceStrength;
    }

}