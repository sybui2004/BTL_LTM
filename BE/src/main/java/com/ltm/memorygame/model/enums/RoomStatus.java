package com.ltm.memorygame.model.enums;

public enum RoomStatus {
    WAITING("WAITING"),
    PLAYING("PLAYING"),
    READY("READY"),
    DELETED("DELETED");

    private final String value;

    RoomStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
