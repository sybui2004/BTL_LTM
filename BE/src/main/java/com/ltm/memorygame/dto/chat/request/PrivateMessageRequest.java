package com.ltm.memorygame.dto.chat.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateMessageRequest {
    @NotNull
    private Long toUserId;

    private Long matchId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}