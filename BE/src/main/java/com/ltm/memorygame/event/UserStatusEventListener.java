package com.ltm.memorygame.event;

import com.ltm.memorygame.tcp.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatusEventListener {
    
    private final TCPServer tcpServer;
    
    @EventListener
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        tcpServer.broadcastUserStatus(event.getUsername(), event.isOnline());
    }
}

