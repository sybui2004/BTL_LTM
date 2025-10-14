package com.ltm.memorygame.dto.chat.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationPreviewDTO {
    private Long otherUserId;
    private String otherUsername;
    private String otherDisplayName;
    private String otherAvatarUrl;

    private Long lastMessageId;
    private String lastMessageText;   
    private String lastMessageType;  
    private Long lastStickerId;       
    private Instant lastMessageTime;

    private boolean lastMessageFromSelf; 
}
