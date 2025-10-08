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
public class WorldMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private String avatarUrl;
    private Instant createdAt;
}