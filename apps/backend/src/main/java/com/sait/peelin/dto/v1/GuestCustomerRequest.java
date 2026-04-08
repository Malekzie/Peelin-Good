package com.sait.peelin.dto.v1;

import com.sait.peelin.validation.GuestContactValid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@GuestContactValid
public class GuestCustomerRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 1)
    private String middleInitial;

    @Size(max = 50)
    private String lastName;

    /** Optional when email is provided; required (10+ digits) when email is blank. */
    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String businessPhone;

    /** Optional when phone is provided; required when phone is blank. */
    @Size(max = 254)
    private String email;

    @Size(max = 120)
    private String addressLine1;

    @Size(max = 120)
    private String addressLine2;

    @Size(max = 120)
    private String city;

    @Size(max = 80)
    private String province;

    @Size(max = 10)
    private String postalCode;
}
