package com.example.memorygame.controller.chat;

import java.util.List;

import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;

public interface ChatContext {
    String getChannelId();
    ChatType getType();
    UserSummary getCurrentUser();
    List<UserSummary> getParticipants();
    boolean canSendMessage();
    
    // UI-related methods
    String getTitle();
    boolean showUserStatus();
    boolean showEmoji();
    boolean showTimestamp();
}