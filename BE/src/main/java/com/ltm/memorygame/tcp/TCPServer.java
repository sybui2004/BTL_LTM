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
    private Thread serverThread;
    private ServerSocket serverSocket;

    @PostConstruct
    public void startServer() {
        serverThread = new Thread(this, "TCP-Server-Thread");
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
}
