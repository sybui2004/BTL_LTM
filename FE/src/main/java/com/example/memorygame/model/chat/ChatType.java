package com.example.memorygame.model.chat;

public enum ChatType {
    PRIVATE(true),    // persistent
    WORLD(false),     // cache only
    MATCH(false),     // temporary
    LOBBY(false);     // temporary
    
    private final boolean persistent;

    ChatType(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Whether messages in this chat type should be persisted to DB.
     */
    public boolean isPersistent() {
        return persistent;
    }
}