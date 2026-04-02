package com.sait.peelin.dto.v1;

import com.sait.peelin.model.BakeryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BakeryUpsertRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 20)
    private String phone;
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
    private BakeryStatus status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    /** Optional full URL to image in Spaces (e.g. CDN …/locations/…). */
    @Size(max = 2048)
    private String bakeryImageUrl;
    @NotNull
    @Valid
    private AddressUpsertRequest address;
}
