package com.ltm.memorygame.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.service.chat.MatchMessageService;
import com.ltm.memorygame.service.chat.PrivateMessageService;
import com.ltm.memorygame.service.chat.StickerService;
import com.ltm.memorygame.service.chat.WorldMessageService;
import com.ltm.memorygame.service.game.GameSessionService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.user.PresenceService;
import com.ltm.memorygame.service.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Factory để tạo ClientHandler cho mỗi TCP connection
 */
@Component
@RequiredArgsConstructor
public class ClientHandlerFactory {

    private final UserService userService;
    private final RoomService roomService;
    private final GameSessionService gameSessionService;
    private final JwtService jwtService;

    private final MatchMessageService matchMessageService;
    private final WorldMessageService worldMessageService;
    private final PresenceService presenceService;
    private final StickerService stickerService;
    
    private final ApplicationContext applicationContext;

    @Value("${tcp.login.requireJwt:false}")
    private boolean requireJwt;

    @Value("${tcp.rateLimit.maxPerSecond:20}")
    private int maxPerSecond;

    /**
     * Tạo ClientHandler mới cho socket connection
     * Lấy PrivateMessageService từ ApplicationContext để tránh circular dependency
     */
    public ClientHandler create(Socket socket, Map<String, ClientHandler> onlineClients) throws IOException {
        PrivateMessageService privateMessageService = applicationContext.getBean(PrivateMessageService.class);
        
        return new ClientHandler(socket, onlineClients,
                userService, roomService, gameSessionService, jwtService, requireJwt, maxPerSecond,
                matchMessageService, privateMessageService, worldMessageService, presenceService, stickerService);
    }
}
