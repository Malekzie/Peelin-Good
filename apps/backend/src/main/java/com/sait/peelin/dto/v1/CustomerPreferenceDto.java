package com.sait.peelin.dto.v1;

import com.sait.peelin.model.PreferenceType;

public record CustomerPreferenceDto (
    Integer tagId,
    String tagName,
    PreferenceType preferenceType,
    Short preferenceStrength
) {}
