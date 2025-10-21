package com.ltm.memorygame.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserStatusChangedEvent extends ApplicationEvent {
    private final String username;
    private final boolean online;

    public UserStatusChangedEvent(Object source, String username, boolean online) {
        super(source);
        this.username = username;
        this.online = online;
    }
}

