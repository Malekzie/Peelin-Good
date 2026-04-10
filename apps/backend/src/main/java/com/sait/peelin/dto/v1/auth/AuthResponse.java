package com.sait.peelin.dto.v1.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class AuthResponse {

    @Getter
    @Setter
    private String token;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String role;

    @Getter
    @Setter
    private UUID userId;

    @Getter
    @Setter
    private String email;

    /** True when a guest customer row exists with this email or optional registration phone. */
    @Getter
    @Setter
    private Boolean priorGuestCheckout;

    @Getter
    @Setter
    private String guestProfileCompletionMessage;

    /** True only when registration just created an employee↔customer link and both accounts are active. */
    @Getter
    @Setter
    private Boolean employeeDiscountLinkEstablished;

    @Getter
    @Setter
    private String employeeDiscountLinkMessage;

    public AuthResponse() {}

    public AuthResponse(String token, String username, String role, UUID userId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }
}
