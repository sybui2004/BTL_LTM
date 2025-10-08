package com.ltm.memorygame.controller.chat;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.ltm.memorygame.dao.game.MatchRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.chat.request.PrivateMessageRequest;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.chat.PrivateMessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    // Gửi tin nhắn private qua STOMP: /app/private.send
@MessageMapping("/private.send")
public void sendMessage(Principal principal, @Valid @Payload PrivateMessageRequest req) {
    Long meId = Long.valueOf(principal.getName());

    User sender = userRepository.findById(meId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
    User receiver = userRepository.findById(req.getToUserId())
            .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
    Match match = (req.getMatchId() != null)
            ? matchRepository.findById(req.getMatchId()).orElse(null)
            : null;

    privateMessageService.sendPrivateMessage(sender, receiver, req.getContent(), match);
}
        // Nếu muốn echo lại cho người gửi (client hiện tin ngay), có thể:
        // privateMessageService.echoToSender(meId, ...) hoặc convertAndSendToUser ở đây.
    // }

    // Lấy lịch sử giữa "me" và otherUserId, trả về cho đúng user gọi
    
    @SendToUser("/queue/private.history")
    public Page<PrivateMessageResponse> getHistory(Principal principal,
                                                   @DestinationVariable Long otherUserId,
                                                   @Header(name = "page", required = false) Integer page,
                                                   @Header(name = "size", required = false) Integer size) {
        Long meId = Long.valueOf(principal.getName());
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 20 : size;
        return privateMessageService.getConversation(meId, otherUserId, p, s);
    }
}
