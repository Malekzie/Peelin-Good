package com.sait.peelin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "employee_specialty")
@IdClass(EmployeeSpecialtyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSpecialty {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "category", nullable = false, length = 64)
    private String category;
}
