package com.example.memorygame.model.game;

/**
 * Game settings model containing theme, size, and time information
 */
public class GameSettings {
    private ThemeDTO theme;
    private String size; // e.g., "5x6", "6x7"
    private String time; // e.g., "20s", "30s", "40s"
    private Long roomId; // Room ID for synchronization
    
    // Additional game info
    private String player1Name;
    private String player2Name;
    private boolean isHost;
    private boolean hostFirstTurn; // true if host goes first, false if guest goes first
    private Long matchId; // Created match ID (for finishMatch)
    
    // Constructors
    public GameSettings() {}
    
    public GameSettings(ThemeDTO theme, String size, String time) {
        this.theme = theme;
        this.size = size;
        this.time = time;
    }
    
    // Getters and setters
    public ThemeDTO getTheme() {
        return theme;
    }
    
    public void setTheme(ThemeDTO theme) {
        this.theme = theme;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public String getPlayer1Name() {
        return player1Name;
    }
    
    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }
    
    public String getPlayer2Name() {
        return player2Name;
    }
    
    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public void setHost(boolean host) {
        isHost = host;
    }
    
    public boolean isHostFirstTurn() {
        return hostFirstTurn;
    }
    
    public void setHostFirstTurn(boolean hostFirstTurn) {
        this.hostFirstTurn = hostFirstTurn;
    }
    
    public Long getMatchId() {
        return matchId;
    }
    
    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    
    @Override
    public String toString() {
        return "GameSettings{" +
                "theme=" + (theme != null ? theme.name : "null") +
                ", size='" + size + '\'' +
                ", time='" + time + '\'' +
                ", roomId=" + roomId +
                ", player1Name='" + player1Name + '\'' +
                ", player2Name='" + player2Name + '\'' +
                ", isHost=" + isHost +
                ", hostFirstTurn=" + hostFirstTurn +
                ", matchId=" + matchId +
                '}';
    }
}
