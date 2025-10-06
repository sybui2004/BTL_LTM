package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
@RequiredArgsConstructor
public class TCPServer {

    private final RoomService roomService;
    private final NotificationService notificationService;
    private final RoomSessionManager sessionManager = new RoomSessionManager();

    private final int PORT = 5000;

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("TCP Server started on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                TCPMessage message = JsonUtil.fromJson(line, TCPMessage.class);
                handleMessage(clientSocket, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Socket socket, TCPMessage message) {
        try {
            switch (message.getType()) {
                case "JOIN":
                    sessionManager.addPlayerToRoom(message.getRoomId(), socket);
                    sessionManager.sendToAll(message.getRoomId(), TCPMessage.builder()
                            .type("UPDATE")
                            .content("Player " + message.getPlayerId() + " joined")
                            .build());
                    break;

                case "EXIT":
                    sessionManager.removePlayerFromRoom(message.getRoomId(), socket);
                    roomService.exitRoom(message.getRoomId(), message.getPlayerId());
                    sessionManager.sendToAll(message.getRoomId(), TCPMessage.builder()
                            .type("UPDATE")
                            .content("Player " + message.getPlayerId() + " exited")
                            .build());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
