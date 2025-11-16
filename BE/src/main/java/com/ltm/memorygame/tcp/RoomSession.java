package com.ltm.memorygame.tcp;


import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomSession {
    private final String roomId;
    private final String owner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    @Setter
    private volatile boolean active = false;
    private final Deque<MatchMessageDTO> messageBuffer = new ConcurrentLinkedDeque<>();
    private static final int MAX_MESSAGES = 200;

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

    
    public void addMessage(MatchMessageDTO message) {
        if (messageBuffer.size() >= MAX_MESSAGES) {
            messageBuffer.pollFirst();
        }
        messageBuffer.addLast(message);
    }
    
    public List<MatchMessageDTO> getHistory() {
        return new java.util.ArrayList<>(messageBuffer);
    }
}
