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
public class PrivateMessageResponse {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Long matchId;
    private String content;
    private MessageType messageType;
    private StickerResponse sticker;
    private Instant createdAt;
}
