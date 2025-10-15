package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.notification.NotificationService;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Map<String, ClientHandler> onlineClients;

    private final UserService userService;
    private final RoomService roomService;
    private final NotificationService notificationService;

    private String username;

    public ClientHandler(Socket socket,
                         Map<String, ClientHandler> onlineClients,
                         UserService userService,
                         RoomService roomService,
                         NotificationService notificationService) throws IOException {
        this.socket = socket;
        this.onlineClients = onlineClients;
        this.userService = userService;
        this.roomService = roomService;
        this.notificationService = notificationService;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String raw;
            while ((raw = in.readLine()) != null) {
                TCPMessage msg = JsonUtil.fromJson(raw, TCPMessage.class);
                handleMessage(msg);
            }
        } catch (IOException e) {
            System.out.println("[WARN] Client disconnected: " + username);
        } finally {
            if (username != null) {
                onlineClients.remove(username);
                broadcastUserStatus(username, false);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }


    private void handleMessage(TCPMessage message) {
        switch (message.getType()) {
            case "LOGIN_REQUEST" -> handleLogin(message);
            case "LOGOUT_REQUEST" -> handleLogout();
            case "WORLD_CHAT" -> handleWorldChat(message);
            case "PRIVATE_CHAT" -> handlePrivateChat(message);
            case "PING" -> sendMessage(new TCPMessage("PONG", null, "server", username));
            default -> System.out.println("[WARN] Unknown type: " + message.getType());
        }
    }

    private void handleLogin(TCPMessage message) {
        Object nameObj = message.getData().get("username");
        if (nameObj == null) {
            sendMessage(new TCPMessage("LOGIN_FAILED",
                    Map.of("reason", "Missing username"), "server", null));
            return;
        }
        this.username = nameObj.toString();
        onlineClients.put(username, this);

        if (!userService.existsByUsername(username)) {
            sendMessage(new TCPMessage("LOGIN_FAILED",
                    Map.of("reason", "Username not found in DB"), "server", null));
            onlineClients.remove(username);
            return;
        }

        System.out.println("[INFO] User logged in: " + username);
        sendMessage(new TCPMessage("LOGIN_SUCCESS", null, "server", username));
        broadcastUserStatus(username, true);
    }

    private void handleLogout() {
        if (username == null) return;
        onlineClients.remove(username);
        broadcastUserStatus(username, false);
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void handleWorldChat(TCPMessage message) {
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(message);
        }
    }

    private void handlePrivateChat(TCPMessage message) {
        String to = message.getReceiver();
        ClientHandler target = onlineClients.get(to);
        if (target != null) {
            target.sendMessage(message);
        } else {
            sendMessage(new TCPMessage("ERROR",
                    Map.of("reason", "User offline"), "server", username));
        }
    }

    private void broadcastUserStatus(String user, boolean online) {
        TCPMessage msg = new TCPMessage("USER_STATUS",
                Map.of("user", user, "online", online), "server", null);
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(msg);
        }
    }

    public void sendMessage(TCPMessage message) {
        synchronized (out) {
            out.println(JsonUtil.toJson(message));
        }
    }
}
