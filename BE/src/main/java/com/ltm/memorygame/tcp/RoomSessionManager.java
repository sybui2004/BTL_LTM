package com.ltm.memorygame.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomSessionManager {
    private static final Map<String, RoomSession> sessions = new ConcurrentHashMap<>();

    public static RoomSession createRoom(String roomId, String owner) {
        RoomSession session = new RoomSession(roomId, owner);
        sessions.put(roomId, session);
        return session;
    }

    public static RoomSession getRoom(String roomId) {
        return sessions.get(roomId);
    }

    public static void removeRoom(String roomId) {
        sessions.remove(roomId);
    }
}
