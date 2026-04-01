package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostChatMessageRequest {
    @Size(max = 2000)
    private String text;
}
