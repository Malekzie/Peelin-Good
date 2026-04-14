package com.sait.peelin.dto.v1.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pre-check before multi-step registration: username/email must not already be in use as a login identity.
 * An email that only matches an employee work email (not their {@code User.user_email} login)
 * remains available so the customer account can be created and auto-linked.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAvailabilityResponse {
    private boolean usernameAvailable;
    private boolean emailAvailable;
}
