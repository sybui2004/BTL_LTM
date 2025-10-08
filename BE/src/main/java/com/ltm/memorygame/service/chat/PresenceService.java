package com.ltm.memorygame.service.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;

    // Lưu trạng thái hiện tại trong RAM
    private final Map<Long, String> presence = new ConcurrentHashMap<>();
    public record PresenceEvent(Long userId, String status) {
    }

    public void setOnline(Long userId) {
        updateStatus(userId, "ONLINE");
    }

    public void setOffline(Long userId) {
        updateStatus(userId, "OFFLINE");
    }

    public void setBusy(Long userId) {
        updateStatus(userId, "BUSY");
    }

    // Lấy trạng thái hiện tại của 1 user; mặc định OFFLINE nếu chưa thấy
    public String getStatus(Long userId) {
        return presence.getOrDefault(userId, "OFFLINE");
    }

    // Snapshot toàn bộ trạng thái (copy để tránh concurrency issues)
    public Map<Long, String> getAllStatuses() {
        return Map.copyOf(presence);
    }
    //update status
    private void updateStatus(Long userId, String status) {
        if (userId == null) {
            return;
        }
        presence.put(userId, status);
        messagingTemplate.convertAndSend("/topic/presence", new PresenceEvent(userId, status));
    }


    // các hàm hỗ trợ được gọi khi người dùng vào/ra khỏi socket, ào trận
    public void onWsConnected(Long userId) {
        setOnline(userId);
    }

    public void onWsDisconnected(Long userId) {
        setOffline(userId);
    }

    public void setBusyForRoom(Long userId, boolean busy) {
        updateStatus(userId, busy ? "BUSY" : "ONLINE");
    }

    
}
