package com.ltm.memorygame.model.enums;

public enum MessageType {
    TEXT ("TEXT"), 
    STICKER ("STICKER");

    private final String value;

    MessageType (String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}