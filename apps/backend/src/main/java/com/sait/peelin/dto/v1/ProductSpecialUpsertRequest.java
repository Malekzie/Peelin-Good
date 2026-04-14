package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or replacing a product special entry.
 * Editable fields only — {@code productSpecialId} (PK) is managed by the server.
 */
@Data
public class ProductSpecialUpsertRequest {

    @NotNull
    private Integer productId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate featuredOn;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal discountPercent = BigDecimal.ZERO;
}
