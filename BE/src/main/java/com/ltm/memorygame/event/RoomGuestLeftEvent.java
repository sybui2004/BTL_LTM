package com.ltm.memorygame.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RoomGuestLeftEvent extends ApplicationEvent {
    private final Long roomId;
    private final String hostUsername;
    
    public RoomGuestLeftEvent(Object source, Long roomId, String hostUsername) {
        super(source);
        this.roomId = roomId;
        this.hostUsername = hostUsername;
    }
}

