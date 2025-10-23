package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.game.GameSessionService;
import com.ltm.memorygame.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClientHandlerFactory {

    private final UserService userService;
    private final RoomService roomService;
    private final GameSessionService gameSessionService;
    private final JwtService jwtService;

    @Value("${tcp.login.requireJwt:false}")
    private boolean requireJwt;

    @Value("${tcp.rateLimit.maxPerSecond:20}")
    private int maxPerSecond;

    public ClientHandler create(Socket socket, Map<String, ClientHandler> onlineClients) throws IOException {
        return new ClientHandler(socket, onlineClients,
                userService, roomService, gameSessionService, jwtService, requireJwt, maxPerSecond);
    }
}
