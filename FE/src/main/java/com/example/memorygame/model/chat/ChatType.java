package com.example.memorygame.model.chat;

public enum ChatType {
    PRIVATE(true),    // persistent
    WORLD(false),     // cache only
    MATCH(false),     // temporary
    LOBBY(false),     // temporary
    STICKER_MATCH(false);   // temporary, for in-game stickers
    
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