package com.ltm.memorygame.dto.chat.response;

import java.time.Instant;

import com.ltm.memorygame.model.enums.MessageType;

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
public class MatchMessageDTO {
    private String roomId;
    private Long fromUserId;
    private String content;
    private Instant createdAt;
    private MessageType messageType;
    private StickerResponse sticker;
}