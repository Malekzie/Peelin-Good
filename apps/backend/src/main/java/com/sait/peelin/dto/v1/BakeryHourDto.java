package com.sait.peelin.dto.v1;

import java.time.LocalTime;

public record BakeryHourDto(
        Integer id,
        short dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {}
