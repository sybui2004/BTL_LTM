package com.ltm.memorygame.model.enums;

public enum UserStatus {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    BUSY("BUSY");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
