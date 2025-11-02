package com.example.memorygame.model.chat;

/**
 * Simple Chat error wrapper used by ChatService listeners.
 */
public class ChatError {
    private final String message;
    private final Throwable cause;

    public ChatError(String message) {
        this(message, null);
    }

    public ChatError(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
