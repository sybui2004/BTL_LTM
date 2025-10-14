package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.notification.NotificationService;
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
    private final NotificationService notificationService;

    public ClientHandler create(Socket socket, Map<String, ClientHandler> onlineClients) throws IOException {
        return new ClientHandler(socket, onlineClients,
                userService, roomService, notificationService);
    }
}
