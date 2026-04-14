package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendStaffMessageRequest(
        @NotBlank @Size(max = 2000) String text
) {}
