package com.sait.peelin.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        int status,
        String message,
        List<String> details,
        OffsetDateTime timestamp
) {}
