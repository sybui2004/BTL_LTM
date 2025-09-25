package com.ltm.memorygame.model.enums;

public enum MatchStatus {
    PLAYING("PLAYING"),
    WIN("WIN"),
    LOSE("LOSE");

    private final String value;

    MatchStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
