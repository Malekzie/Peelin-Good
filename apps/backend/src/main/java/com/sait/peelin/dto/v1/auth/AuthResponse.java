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

    public AuthResponse() {}

    public AuthResponse(String token, String username, String role, UUID userId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }
}
