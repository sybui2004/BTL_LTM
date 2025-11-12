package com.ltm.memorygame.model.enums;

public enum InviteStatus {
    PENDING("PENDING"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED");
    private final String value;

    InviteStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
