package com.sait.peelin.dto.v1;

import com.sait.peelin.model.PreferenceType;
import lombok.Data;

import java.util.List;

@Data
public class CustomerPreferenceSaveRequest {
    private List<PreferenceEntry> preferences;

    @Data
    public static class PreferenceEntry {
        private Integer tagId;
        private PreferenceType preferenceType;
    }
}
