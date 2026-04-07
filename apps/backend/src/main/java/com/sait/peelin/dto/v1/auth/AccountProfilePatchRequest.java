package com.sait.peelin.dto.v1.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * Partial update for the authenticated user's username and/or sign-in email.
 * Null fields are left unchanged.
 */
@Getter
@Setter
public class AccountProfilePatchRequest {
    private String username;
    private String email;
}
