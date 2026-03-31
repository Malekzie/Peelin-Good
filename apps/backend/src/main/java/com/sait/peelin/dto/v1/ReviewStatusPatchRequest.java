package com.sait.peelin.dto.v1;

import com.sait.peelin.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewStatusPatchRequest {
    @NotNull
    private ReviewStatus status;
}
