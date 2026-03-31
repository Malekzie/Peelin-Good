package com.sait.peelin.dto.v1;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OrderDeliveredPatchRequest {
    private OffsetDateTime deliveredAt;
}
