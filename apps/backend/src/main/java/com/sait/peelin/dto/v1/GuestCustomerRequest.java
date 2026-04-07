package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestCustomerRequest {

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @Size(max = 1)
    @Pattern(regexp = "^$|^[A-Za-z]$", message = "Middle initial must be a single letter")
    private String middleInitial;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String businessPhone;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

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
