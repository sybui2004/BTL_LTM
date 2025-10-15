package com.ltm.memorygame.tcp;

import lombok.Getter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class RoomSession {
    private final String roomId;
    private final String owner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private volatile boolean active = false;

    public RoomSession(String roomId, String owner) {
        this.roomId = roomId;
        this.owner = owner;
        members.add(owner);
    }

    public void addMember(String username) {
        members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public void setActive(boolean active) { this.active = active; }
}
