package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagCreateRequest {
    @NotBlank
    @Size(max = 50)
    private String name;

    private boolean dietary;
}
