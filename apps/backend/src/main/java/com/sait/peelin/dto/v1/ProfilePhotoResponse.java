package com.sait.peelin.dto.v1;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePhotoResponse {
    private String profilePhotoPath;
    private boolean photoApprovalPending;
}
