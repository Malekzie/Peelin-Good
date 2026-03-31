package com.sait.peelin.dto.v1.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** Optional; when blank, email is used as the login principal. */
    private String username;

    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
