package com.ltm.memorygame.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HostPromotedEvent extends ApplicationEvent {
    private final Long roomId;
    private final String newHostUsername;

    public HostPromotedEvent(Object source, Long roomId, String newHostUsername) {
        super(source);
        this.roomId = roomId;
        this.newHostUsername = newHostUsername;
    }

}

