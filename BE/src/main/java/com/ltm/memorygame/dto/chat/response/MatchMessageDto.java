package com.ltm.memorygame.dto.chat.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchMessageDto {
    private String roomId;
    private Long fromUserId;
    private String content;
    private Instant createdAt;
}