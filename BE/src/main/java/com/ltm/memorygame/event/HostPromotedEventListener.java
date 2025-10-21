package com.ltm.memorygame.event;

import com.ltm.memorygame.tcp.TCPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HostPromotedEventListener {
    
    @Autowired
    private TCPServer tcpServer;
    
    @EventListener
    public void handleHostPromoted(HostPromotedEvent event) {
        tcpServer.sendHostPromotedNotification(event.getNewHostUsername(), event.getRoomId());
    }
}

