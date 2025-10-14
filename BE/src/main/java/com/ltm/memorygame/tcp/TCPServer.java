package com.ltm.memorygame.tcp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TCPServer implements Runnable {

    private static final int PORT = 12345;
    private final ConcurrentHashMap<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    private final ClientHandlerFactory handlerFactory;
    private volatile boolean running = true;
    private Thread serverThread;
    private ServerSocket serverSocket;

    @PostConstruct
    public void startServer() {
        serverThread = new Thread(this, "TCP-Server-Thread");
        serverThread.start();
        System.out.println("[TCP] Server thread started.");
    }

    @Override
    public void run() {
        System.out.println("[TCP] Listening on port " + PORT + "...");
        try (ServerSocket ss = new ServerSocket(PORT)) {
            this.serverSocket = ss;
            while (running) {
                Socket clientSocket = ss.accept();
                System.out.println("[TCP] New connection from " + clientSocket.getInetAddress());
                try {
                    ClientHandler handler = handlerFactory.create(clientSocket, onlineClients);
                    handler.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[TCP] Server error: " + e.getMessage());
            } else {
                System.out.println("[TCP] Server stopped gracefully.");
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
            e.printStackTrace();
        }
        System.out.println("[TCP] Server stopped.");
    }
}
