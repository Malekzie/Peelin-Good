package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductUpsertRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @Size(max = 1000)
    private String description;
    @NotNull
    @PositiveOrZero
    private BigDecimal basePrice;
    @Size(max = 500)
    private String imageUrl;
    private List<Integer> tagIds;
}
