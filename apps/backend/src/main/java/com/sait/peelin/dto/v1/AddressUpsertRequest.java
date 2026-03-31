package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressUpsertRequest {
    @NotBlank
    @Size(max = 120)
    private String line1;
    @Size(max = 120)
    private String line2;
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
