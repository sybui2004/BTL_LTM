package com.ltm.memorygame.dao.chat;

import java.time.LocalDateTime;

public interface ConversationPreviewProjection {
    Long getLastMessageId();
    String getLastMessageText();
    String getLastMessageType();  
    Long getLastStickerId();
    LocalDateTime getLastMessageTime();

    Long getSenderId();
    Long getReceiverId();

    Long getOtherUserId();
    String getOtherUsername();
    String getOtherDisplayName();
    String getOtherAvatarUrl();
}
