package com.ltm.memorygame.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.service.chat.MatchMessageService;
import com.ltm.memorygame.service.chat.PrivateMessageService;
import com.ltm.memorygame.service.chat.WorldMessageService;
import com.ltm.memorygame.service.notification.NotificationService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.user.PresenceService;
import com.ltm.memorygame.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientHandlerFactory {

    private final UserService userService;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    private final MatchMessageService matchMessageService;
    private final PrivateMessageService privateMessageService;
    private final WorldMessageService worldMessageService;
    private final PresenceService presenceService;

    @Value("${tcp.login.requireJwt:false}")
    private boolean requireJwt;

    @Value("${tcp.rateLimit.maxPerSecond:20}")
    private int maxPerSecond;

    public ClientHandler create(Socket socket, Map<String, ClientHandler> onlineClients) throws IOException {
        return new ClientHandler(socket, onlineClients,
                userService, roomService, notificationService, jwtService, requireJwt, maxPerSecond,
                matchMessageService, privateMessageService, worldMessageService, presenceService);
                
    }
}
