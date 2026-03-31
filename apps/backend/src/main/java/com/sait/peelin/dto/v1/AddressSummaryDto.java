package com.sait.peelin.dto.v1;

public record AddressSummaryDto(
        Integer id,
        String line1,
        String line2,
        String city,
        String province,
        String postalCode
) {}
