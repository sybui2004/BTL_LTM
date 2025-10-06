package com.ltm.memorygame.tcp;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class RoomSessionManager {

    // roomId -> list 2 player sockets
    private final Map<Long, List<Socket>> roomSockets = new HashMap<>();

    public void addPlayerToRoom(Long roomId, Socket socket) {
        roomSockets.computeIfAbsent(roomId, k -> new ArrayList<>());
        roomSockets.get(roomId).add(socket);
    }

    public void removePlayerFromRoom(Long roomId, Socket socket) {
        List<Socket> list = roomSockets.get(roomId);
        if (list != null) {
            list.remove(socket);
            if (list.isEmpty()) roomSockets.remove(roomId);
        }
    }

    public List<Socket> getPlayers(Long roomId) {
        return roomSockets.getOrDefault(roomId, Collections.emptyList());
    }

    public void sendToAll(Long roomId, TCPMessage message) {
        List<Socket> players = getPlayers(roomId);
        for (Socket socket : players) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(JsonUtil.toJson(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
