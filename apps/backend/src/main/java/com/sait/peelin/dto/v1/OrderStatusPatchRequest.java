package com.sait.peelin.dto.v1;

import com.sait.peelin.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusPatchRequest {
    @NotNull
    private OrderStatus status;
}
