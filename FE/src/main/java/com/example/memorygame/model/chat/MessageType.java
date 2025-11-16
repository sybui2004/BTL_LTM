package com.example.memorygame.model.chat;

/**
 * Enum này phải match chính xác với MessageType của BE
 */
public enum MessageType {
    TEXT,       // Tin nhắn văn bản
    STICKER,    // Sticker
    SYSTEM,     // Tin nhắn hệ thống
    GAME_EVENT  // Events từ game
}