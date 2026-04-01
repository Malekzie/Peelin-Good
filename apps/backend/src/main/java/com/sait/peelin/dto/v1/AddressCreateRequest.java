package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressCreateRequest(
        @NotBlank @Size(max = 120) String line1,
        @Size(max = 120) String line2,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 80) String province,
        @NotBlank @Size(max = 10) String postalCode
) {}
