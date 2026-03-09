package com.sait.peelin.model;

import jakarta.persistence.*;

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

    @Column(name = "preference_type", columnDefinition = "preference_type not null")
    private Object preferenceType;

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

    public Object getPreferenceType() {
        return preferenceType;
    }

    public void setPreferenceType(Object preferenceType) {
        this.preferenceType = preferenceType;
    }

    public Short getPreferenceStrength() {
        return preferenceStrength;
    }

    public void setPreferenceStrength(Short preferenceStrength) {
        this.preferenceStrength = preferenceStrength;
    }

}