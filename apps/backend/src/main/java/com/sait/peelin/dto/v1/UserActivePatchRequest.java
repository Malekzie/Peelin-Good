package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserActivePatchRequest {
    @NotNull
    private Boolean active;
}
