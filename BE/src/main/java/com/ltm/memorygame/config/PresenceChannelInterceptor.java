package com.ltm.memorygame.config;

import java.security.Principal;

import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.ltm.memorygame.service.chat.PresenceService;

@Component
public class PresenceChannelInterceptor implements ChannelInterceptor {

    private final PresenceService presenceService;

    public PresenceChannelInterceptor(@Lazy PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        Principal principal = accessor.getUser();
        if (principal == null) return message;

        // Principal#getName() đã được set từ X-User-Id ở interceptor hiện có trong WebSocketConfig
        String userIdStr = principal.getName();
        if (userIdStr == null) return message;

        try {
            Long userId = Long.parseLong(userIdStr);
            if (StompCommand.CONNECT.equals(command)) {
                presenceService.onWsConnected(userId);
            } else if (StompCommand.DISCONNECT.equals(command)) {
                presenceService.onWsDisconnected(userId);
            }
        } catch (NumberFormatException ignored) {
            // Nếu Principal không phải là số (không đúng kỳ vọng), bỏ qua để tránh ném lỗi ở pipeline WS.
        }

        return message;
    }
}
