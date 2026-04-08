package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creates the customer's profile (name, phones, address) for a user that registered without one.
 * Customer email is taken from the authenticated user's account email.
 */
@Getter
@Setter
public class CustomerBootstrapRequest {

    @NotBlank
    @Size(max = 50)
    private String firstName;

    /** Optional: single letter (e.g. J). */
    @Size(max = 1)
    @Pattern(regexp = "^$|^[A-Za-z]$", message = "Middle initial must be a single letter")
    private String middleInitial;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    private String phone;

    /** Optional; stored on customer when provided. */
    @Size(max = 20)
    private String businessPhone;

    @NotBlank
    @Size(max = 120)
    private String addressLine1;

    @Size(max = 120)
    private String addressLine2;

    @NotBlank
    @Size(max = 120)
    private String city;

    @NotBlank
    @Size(max = 80)
    private String province;

    @NotBlank
    @Size(max = 10)
    private String postalCode;
}
