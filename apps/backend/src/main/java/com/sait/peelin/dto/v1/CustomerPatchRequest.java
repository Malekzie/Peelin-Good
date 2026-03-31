package com.sait.peelin.dto.v1;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class CustomerPatchRequest {
    private Integer rewardBalance;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String phone;
    private String businessPhone;
    private String email;
    private Integer addressId;
    /** When set, creates or updates the customer's address (line2 optional). */
    @Valid
    private AddressUpsertRequest address;
    private Integer rewardTierId;
    private Boolean photoApprovalPending;
}
