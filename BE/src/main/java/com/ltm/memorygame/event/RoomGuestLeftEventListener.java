package com.ltm.memorygame.event;

import com.ltm.memorygame.tcp.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomGuestLeftEventListener {
    private final TCPServer tcpServer;
    
    @EventListener
    public void handleRoomGuestLeft(RoomGuestLeftEvent event) {
        tcpServer.sendGuestLeftNotification(event.getHostUsername(), event.getRoomId());
    }
}

