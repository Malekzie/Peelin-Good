package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"user\"", uniqueConstraints = {
        @UniqueConstraint(name = "user_username_key",
                columnNames = {"username"}),
        @UniqueConstraint(name = "user_user_email_key",
                columnNames = {"user_email"})})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", nullable = false)
    private UUID id;

    @Size(max = 50)
    @NotNull
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Size(max = 254)
    @NotNull
    @Column(name = "user_email", nullable = false, length = 254)
    private String userEmail;

    @Size(max = 255)
    @NotNull
    @Column(name = "user_password_hash", nullable = false)
    private String userPasswordHash;

    @Column(name = "user_role", columnDefinition = "user_role not null")
    private Object userRole;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "user_created_at", nullable = false)
    private OffsetDateTime userCreatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPasswordHash() {
        return userPasswordHash;
    }

    public void setUserPasswordHash(String userPasswordHash) {
        this.userPasswordHash = userPasswordHash;
    }

    public Object getUserRole() {
        return userRole;
    }

    public void setUserRole(Object userRole) {
        this.userRole = userRole;
    }

    public OffsetDateTime getUserCreatedAt() {
        return userCreatedAt;
    }

    public void setUserCreatedAt(OffsetDateTime userCreatedAt) {
        this.userCreatedAt = userCreatedAt;
    }

}