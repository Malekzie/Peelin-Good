package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewCreateRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Short rating;
    @Size(max = 2000)
    private String comment;
    private UUID orderId;
}
