package com.ltm.memorygame.tcp;

import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;

import lombok.Getter;

@Getter
public class RoomSession {
    private final String roomId;
    private final String owner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
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

    public void setActive(boolean active) { this.active = active; }

    public void addMessage(MatchMessageDTO message) {
        if (messageBuffer.size() >= MAX_MESSAGES) {
            messageBuffer.pollFirst();
        }
        messageBuffer.addLast(message);
    }
    
    public List<MatchMessageDTO> getHistory() {
        return messageBuffer.stream()
                .sorted(Comparator.comparing(MatchMessageDTO::getCreatedAt))
                .collect(Collectors.toList());
    }
}
