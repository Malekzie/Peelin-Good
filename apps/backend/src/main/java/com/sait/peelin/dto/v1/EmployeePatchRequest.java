package com.sait.peelin.dto.v1;

public record EmployeePatchRequest(
        String firstName,
        String middleInitial,
        String lastName,
        String phone,
        String businessPhone,
        String workEmail,
        Integer addressId,
        AddressUpsertRequest address
) {}
