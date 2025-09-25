package com.ltm.memorygame.model.enums;

public enum FriendStatus {
    PENDING("PENDING"),
    ACCEPTED("ACCEPTED");

    private final String value;

    FriendStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
