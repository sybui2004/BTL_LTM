package com.example.memorygame.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * TCP Client for real-time communication with the server.
 * Handles login, status updates, and chat messages.
 */
public class TCPClient {
    private static TCPClient instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private String host = "localhost"; // Default host
    private int port = 12345; // Default port (must match BE tcp.server.port)
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = false;
    
    private final Map<String, Consumer<TCPMessage>> messageHandlers = new ConcurrentHashMap<>();
    
    private TCPClient() {
    }
    
    public static TCPClient getInstance() {
        if (instance == null) {
            synchronized (TCPClient.class) {
                if (instance == null) {
                    instance = new TCPClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a handler for a specific message type
     */
    public void onMessage(String messageType, Consumer<TCPMessage> handler) {
        messageHandlers.put(messageType, handler);
    }
    
    /**
     * Remove a handler for a specific message type
     */
    public void removeHandler(String messageType) {
        messageHandlers.remove(messageType);
    }
    
    /**
     * Connect and login to TCP server
     */
    public boolean connect(String username, String token) {
        try {
            if (socket != null && !socket.isClosed()) {
                System.out.println("[TCP] Already connected");
                return true;
            }
            
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Start listener thread
            running = true;
            Thread listenerThread = new Thread(this::listen, "TCP-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            // Send login request
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("token", token);
            
            TCPMessage loginMsg = new TCPMessage("LOGIN_REQUEST", loginData, username, null);
            sendMessage(loginMsg);
            
            System.out.println("[TCP] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[TCP] Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        running = false;
        try {
            if (out != null) {
                TCPMessage logoutMsg = new TCPMessage("LOGOUT_REQUEST", null, null, null);
                sendMessage(logoutMsg);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[TCP] Disconnected");
        } catch (IOException e) {
            System.err.println("[TCP] Error during disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Send a message to the server
     */
    public void sendMessage(TCPMessage message) {
        if (out != null && socket != null && !socket.isClosed()) {
            try {
                String json = MAPPER.writeValueAsString(message);
                System.out.println("[TCP] Sending message type: " + message.getType() + ", JSON: " + json);
                out.println(json);
                out.flush(); // Ensure message is sent immediately
                System.out.println("[TCP] ✓ Message sent successfully: " + message.getType());
            } catch (Exception e) {
                System.err.println("[TCP] Failed to send message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[TCP] Not connected, cannot send message. Socket closed: " + (socket == null || socket.isClosed()));
        }
    }
    
    /**
     * Listen for incoming messages from server
     */
    private void listen() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                try {
                    TCPMessage message = MAPPER.readValue(line, TCPMessage.class);
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("[TCP] Failed to parse message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[TCP] Connection lost: " + e.getMessage());
            }
        } finally {
            running = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Handle incoming message by calling registered handlers
     */
    private void handleMessage(TCPMessage message) {
        String type = message.getType();
        System.out.println("[TCP] Received: " + type + " | Data: " + message.getData());
        System.out.println("[TCP] Registered handlers: " + messageHandlers.keySet());
        
        Consumer<TCPMessage> handler = messageHandlers.get(type);
        if (handler != null) {
            System.out.println("[TCP] ✓ Handler found for " + type + ", executing on JavaFX thread");
            // Execute handler on JavaFX Application Thread
            Platform.runLater(() -> {
                try {
                    handler.accept(message);
                    System.out.println("[TCP] ✓ Handler executed successfully for " + type);
                } catch (Exception e) {
                    System.err.println("[TCP] ✗ Error executing handler for " + type + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.err.println("[TCP] ✗ No handler registered for: " + type);
            System.err.println("[TCP] Available handlers: " + messageHandlers.keySet());
        }
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && running;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * TCP Message structure matching backend
     */
    public static class TCPMessage {
        private String type;
        private Map<String, Object> data;
        private String sender;
        private String receiver;
        private long timestamp;
        private String status;
        
        public TCPMessage() {
        }
        
        public TCPMessage(String type, Map<String, Object> data, String sender, String receiver) {
            this.type = type;
            this.data = data;
            this.sender = sender;
            this.receiver = receiver;
            this.timestamp = System.currentTimeMillis();
            this.status = "OK";
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

