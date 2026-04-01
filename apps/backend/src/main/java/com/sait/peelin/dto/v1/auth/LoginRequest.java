package com.sait.peelin.dto.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class LoginRequest {

    /** Optional; when blank, email is used as the login principal. */
    private String username;

    private String email;

    @NotBlank
    private String password;

    @AssertTrue(message = "username or email must be provided")
    public boolean isLoginPrincipalProvided() {
        return (username != null && !username.trim().isEmpty())
                || (email != null && !email.trim().isEmpty());
    }
}
