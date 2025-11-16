package com.example.memorygame.model.chat;

import java.time.Duration;

public class ChatConfig {
    public static final int WORLD_CHAT_LIMIT = 100;
    public static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(30);
    public static final int MAX_MESSAGE_LENGTH = 500;
}