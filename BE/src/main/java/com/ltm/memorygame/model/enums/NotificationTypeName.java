package com.ltm.memorygame.model.enums;

public enum NotificationTypeName {
    FRIEND_REQUEST_RECEIVED("You received a friend request"),
    FRIEND_REQUEST_ACCEPTED("Your friend request was accepted"),
    FRIEND_ONLINE("Your friend is online"),
    MATCH_INVITE_RECEIVED("You received a match invite"),
    MATCH_INVITE_DECLINED("Your match invite was declined"),
    ACCOUNT_CREATED("Your account has been successfully created"),
    ACCOUNT_UPDATED("Your account information has been updated"),
    PASSWORD_CHANGED("Your password has been changed");

    private final String value;

    NotificationTypeName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
