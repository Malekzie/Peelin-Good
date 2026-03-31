package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SendLegacyMessageRequest {
    @NotNull
    private UUID receiverId;
    @NotBlank
    @Size(max = 255)
    private String subject;
    @NotBlank
    @Size(max = 2000)
    private String content;
}
