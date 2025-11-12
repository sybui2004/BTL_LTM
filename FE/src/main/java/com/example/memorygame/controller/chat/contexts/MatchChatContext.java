package com.example.memorygame.controller.chat.contexts;

import java.util.Arrays;
import java.util.List;

import com.example.memorygame.controller.chat.ChatContext;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;

/**
 * Context cho Match Chat - chat trong trận đấu
 * 
 * Đặc điểm:
 * - Chat giữa 2 players trong match
 * - Temporary (xóa sau khi match kết thúc)
 * - UI tích hợp với match screen
 * - Phụ thuộc match state (chỉ chat khi đang chơi)
 */
public class MatchChatContext implements ChatContext {
    private final String matchId;
    private final UserSummary currentUser;
    private final UserSummary opponent;
    private boolean matchActive;
    
    public MatchChatContext(String matchId, UserSummary currentUser, UserSummary opponent) {
        this.matchId = matchId;
        this.currentUser = currentUser;
        this.opponent = opponent;
        this.matchActive = true;
    }
    
    @Override
    public String getChannelId() {
        return "match_" + matchId;
    }
    
    @Override
    public ChatType getType() {
        return ChatType.MATCH;
    }
    
    @Override
    public UserSummary getCurrentUser() {
        return currentUser;
    }
    
    @Override
    public List<UserSummary> getParticipants() {
        return Arrays.asList(currentUser, opponent);
    }
    
    @Override
    public boolean canSendMessage() {
        // Chỉ cho phép chat khi match đang active
        return matchActive && currentUser != null && opponent != null;
    }
    
    @Override
    public String getTitle() {
        return "Match Chat";
    }
    
    @Override
    public boolean showUserStatus() {
        // Hiển thị avatar và tên players
        return true;
    }
    
    @Override
    public boolean showEmoji() {
        // Hỗ trợ quick reactions
        return true;
    }
    
    @Override
    public boolean showTimestamp() {
        return false; // Không cần timestamp trong match
    }
    
    // Getters/setters cho match state
    public String getMatchId() {
        return matchId;
    }
    
    public UserSummary getOpponent() {
        return opponent;
    }
    
    public void setMatchActive(boolean active) {
        this.matchActive = active;
    }
    
    public boolean isMatchActive() {
        return matchActive;
    }
}
