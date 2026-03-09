package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "employee_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bakery_id", nullable = false)
    private Bakery bakery;

    @Size(max = 50)
    @NotNull
    @Column(name = "employee_first_name", nullable = false, length = 50)
    private String employeeFirstName;

    @Size(max = 2)
    @Column(name = "employee_middle_initial", columnDefinition = "CHAR(2)")
    private String employeeMiddleInitial;

    @Size(max = 50)
    @NotNull
    @Column(name = "employee_last_name", nullable = false, length = 50)
    private String employeeLastName;

    @Size(max = 40)
    @NotNull
    @Column(name = "employee_position", nullable = false, length = 40)
    private String employeePosition;

    @Size(max = 20)
    @NotNull
    @Column(name = "employee_phone", nullable = false, length = 20)
    private String employeePhone;

    @Size(max = 20)
    @Column(name = "employee_business_phone", length = 20)
    private String employeeBusinessPhone;

    @Size(max = 254)
    @NotNull
    @Column(name = "employee_work_email", nullable = false, length = 254)
    private String employeeWorkEmail;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Bakery getBakery() {
        return bakery;
    }

    public void setBakery(Bakery bakery) {
        this.bakery = bakery;
    }

    public String getEmployeeFirstName() {
        return employeeFirstName;
    }

    public void setEmployeeFirstName(String employeeFirstName) {
        this.employeeFirstName = employeeFirstName;
    }

    public String getEmployeeMiddleInitial() {
        return employeeMiddleInitial;
    }

    public void setEmployeeMiddleInitial(String employeeMiddleInitial) {
        this.employeeMiddleInitial = employeeMiddleInitial;
    }

    public String getEmployeeLastName() {
        return employeeLastName;
    }

    public void setEmployeeLastName(String employeeLastName) {
        this.employeeLastName = employeeLastName;
    }

    public String getEmployeePosition() {
        return employeePosition;
    }

    public void setEmployeePosition(String employeePosition) {
        this.employeePosition = employeePosition;
    }

    public String getEmployeePhone() {
        return employeePhone;
    }

    public void setEmployeePhone(String employeePhone) {
        this.employeePhone = employeePhone;
    }

    public String getEmployeeBusinessPhone() {
        return employeeBusinessPhone;
    }

    public void setEmployeeBusinessPhone(String employeeBusinessPhone) {
        this.employeeBusinessPhone = employeeBusinessPhone;
    }

    public String getEmployeeWorkEmail() {
        return employeeWorkEmail;
    }

    public void setEmployeeWorkEmail(String employeeWorkEmail) {
        this.employeeWorkEmail = employeeWorkEmail;
    }

}