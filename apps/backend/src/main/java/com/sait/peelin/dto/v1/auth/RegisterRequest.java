package com.sait.peelin.dto.v1.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    /**
     * Optional; if provided, used to detect a prior guest checkout by phone when linking at registration.
     */
    @Size(max = 30)
    private String phone;

    @NotBlank
    private String password;
}
