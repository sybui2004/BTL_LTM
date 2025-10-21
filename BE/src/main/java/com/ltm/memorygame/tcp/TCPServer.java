package com.ltm.memorygame.tcp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TCPServer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TCPServer.class);

    @Value("${tcp.server.port:12345}")
    private int port;

    private final ConcurrentHashMap<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    private final ClientHandlerFactory handlerFactory;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    @PostConstruct
    public void startServer() {
        Thread serverThread = new Thread(this, "TCP-Server-Thread");
        serverThread.start();
        log.info("[TCP] Server thread started.");
    }

    @Override
    public void run() {
        log.info("[TCP] Listening on port {}...", port);
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            while (running) {
                Socket clientSocket = ss.accept();
                log.info("[TCP] New connection from {}", clientSocket.getInetAddress());
                try {
                    ClientHandler handler = handlerFactory.create(clientSocket, onlineClients);
                    handler.start();
                } catch (IOException e) {
                    log.error("[TCP] Failed to create handler: {}", e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            if (running) {
                log.error("[TCP] Server error: {}", e.getMessage());
            } else {
                log.info("[TCP] Server stopped gracefully.");
            }
        }
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.warn("[TCP] Error during server socket close: {}", e.getMessage());
        }
        // attempt to shutdown active clients
        try {
            for (ClientHandler handler : onlineClients.values()) {
                try { handler.shutdown(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        log.info("[TCP] Server stopped.");
    }

    /**
     * Broadcast user status change to all connected clients
     */
    public void broadcastUserStatus(String username, boolean online) {
        TCPMessage msg = new TCPMessage("USER_STATUS",
                java.util.Map.of("user", username, "online", online), "server", null);
        for (ClientHandler handler : onlineClients.values()) {
            try {
                handler.sendMessage(msg);
            } catch (Exception e) {
                log.warn("[TCP] Failed to broadcast to client: {}", e.getMessage());
            }
        }
        log.info("[TCP] Broadcasted status: {} -> {}", username, online ? "ONLINE" : "OFFLINE");
    }

    /**
     * Send invite notification to a specific user
     */
    public void sendInviteNotification(String receiverUsername, Long roomId, String senderName) {
        ClientHandler handler = onlineClients.get(receiverUsername);
        if (handler != null) {
            TCPMessage msg = new TCPMessage("INVITE_RECEIVED",
                    java.util.Map.of("roomId", roomId, "senderName", senderName), 
                    "server", receiverUsername);
            try {
                handler.sendMessage(msg);
                log.info("[TCP] Sent invite notification to {}: room {} from {}", receiverUsername, roomId, senderName);
            } catch (Exception e) {
                log.warn("[TCP] Failed to send invite to {}: {}", receiverUsername, e.getMessage());
            }
        } else {
            log.debug("[TCP] User {} not online, skipping invite notification", receiverUsername);
        }
    }
    
    /**
     * Send room update notification when guest joins (to host)
     */
    public void sendRoomUpdateNotification(String hostUsername, Long roomId, Long guestId, String guestDisplayName, String guestAvatarUrl) {
        ClientHandler handler = onlineClients.get(hostUsername);
        if (handler != null) {
            TCPMessage msg = new TCPMessage("ROOM_UPDATED",
                    java.util.Map.of(
                        "roomId", roomId,
                        "guestId", guestId,
                        "guestDisplayName", guestDisplayName,
                        "guestAvatarUrl", guestAvatarUrl != null ? guestAvatarUrl : ""
                    ), 
                    "server", hostUsername);
            try {
                handler.sendMessage(msg);
                log.info("[TCP] Sent room update to {}: guest {} joined room {}", hostUsername, guestDisplayName, roomId);
            } catch (Exception e) {
                log.warn("[TCP] Failed to send room update to {}: {}", hostUsername, e.getMessage());
            }
        } else {
            log.debug("[TCP] Host {} not online, skipping room update notification", hostUsername);
        }
    }
    
    /**
     * Send room joined notification (to guest who just accepted invite)
     */
    public void sendRoomJoinedNotification(String guestUsername, Long roomId, Long hostId, String hostDisplayName, String hostAvatarUrl) {
        ClientHandler handler = onlineClients.get(guestUsername);
        if (handler != null) {
            TCPMessage msg = new TCPMessage("ROOM_JOINED",
                    java.util.Map.of(
                        "roomId", roomId,
                        "hostId", hostId,
                        "hostDisplayName", hostDisplayName,
                        "hostAvatarUrl", hostAvatarUrl != null ? hostAvatarUrl : ""
                    ),
                    "server", guestUsername);
            try {
                handler.sendMessage(msg);
                log.info("[TCP] Sent room joined to {}: joined room {} with host {}", guestUsername, roomId, hostDisplayName);
            } catch (Exception e) {
                log.warn("[TCP] Failed to send room joined to {}: {}", guestUsername, e.getMessage());
            }
        } else {
            log.debug("[TCP] Guest {} not online, skipping room joined notification", guestUsername);
        }
    }
    
    /**
     * Send guest left notification (to host when guest disconnects)
     */
    public void sendGuestLeftNotification(String hostUsername, Long roomId) {
        ClientHandler handler = onlineClients.get(hostUsername);
        if (handler != null) {
            TCPMessage msg = new TCPMessage("GUEST_LEFT",
                    java.util.Map.of("roomId", roomId),
                    "server", hostUsername);
            try {
                handler.sendMessage(msg);
                log.info("[TCP] Sent guest left notification to {} for room {}", hostUsername, roomId);
            } catch (Exception e) {
                log.warn("[TCP] Failed to send guest left to {}: {}", hostUsername, e.getMessage());
            }
        } else {
            log.debug("[TCP] Host {} not online, skipping guest left notification", hostUsername);
        }
    }
    
    /**
     * Send host promoted notification (to new host when old host disconnects)
     */
    public void sendHostPromotedNotification(String newHostUsername, Long roomId) {
        ClientHandler handler = onlineClients.get(newHostUsername);
        if (handler != null) {
            TCPMessage msg = new TCPMessage("HOST_PROMOTED",
                    java.util.Map.of("roomId", roomId),
                    "server", newHostUsername);
            try {
                handler.sendMessage(msg);
                log.info("[TCP] Sent host promoted notification to {} for room {}", newHostUsername, roomId);
            } catch (Exception e) {
                log.warn("[TCP] Failed to send host promoted to {}: {}", newHostUsername, e.getMessage());
            }
        } else {
            log.debug("[TCP] New host {} not online, skipping host promoted notification", newHostUsername);
        }
    }
}
