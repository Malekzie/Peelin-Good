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
@Table(name = "\"user\"", uniqueConstraints = {
        @UniqueConstraint(name = "user_username_key",
                columnNames = {"username"}),
        @UniqueConstraint(name = "user_user_email_key",
                columnNames = {"user_email"})})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

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

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "user_role", nullable = false, columnDefinition = "user_role")
    private UserRole userRole;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "user_created_at", nullable = false)
    private OffsetDateTime userCreatedAt;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Size(max = 500)
    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "photo_approval_pending", nullable = false)
    private Boolean photoApprovalPending;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public OffsetDateTime getUserCreatedAt() {
        return userCreatedAt;
    }

    public void setUserCreatedAt(OffsetDateTime userCreatedAt) {
        this.userCreatedAt = userCreatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
