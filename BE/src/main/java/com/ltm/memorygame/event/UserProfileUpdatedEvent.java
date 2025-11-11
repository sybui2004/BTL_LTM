package com.ltm.memorygame.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserProfileUpdatedEvent extends ApplicationEvent {
    private final String username;
    private final Long userId;

    public UserProfileUpdatedEvent(Object source, String username, Long userId) {
        super(source);
        this.username = username;
        this.userId = userId;
    }
}
