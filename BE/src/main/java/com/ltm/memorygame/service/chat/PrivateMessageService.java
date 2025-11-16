package com.ltm.memorygame.service.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.chat.PrivateMessageRepository;
import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.dto.chat.response.ConversationPreviewDTO;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.chat.PrivateMessage;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.user.FriendService;
import com.ltm.memorygame.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final PrivateMessageRepository privateMessageRepository;
    private final UserService userService;
    private final StickerRepository stickerRepository;
    private final FriendService friendService;
    
    // Lưu tin nhắn riêng vào db
    public PrivateMessageResponse sendPrivateMessage(Long senderId, Long receiverId, String content, Match match, MessageType type,
            Long stickerId) {
        // Validation theo loại message
        if (type == MessageType.TEXT) {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Message content cannot be empty for TEXT messages");
            }
        } else if (type == MessageType.STICKER) {
            // Cho phép content rỗng với STICKER, nhưng cần stickerId hợp lệ
            if (stickerId == null) {
                throw new IllegalArgumentException("Sticker id is required for STICKER messages");
            }
        }

        Sticker sticker = null;
        if (stickerId != null) {
            sticker = stickerRepository.findById(stickerId)
                    .orElseThrow(() -> new IllegalArgumentException("Sticker not found: " + stickerId));
        }

        User sender = userService.getEntityById(senderId);
        User receiver = userService.getEntityById(receiverId);

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMatch(match);
        // DB column `content` is NOT NULL, đảm bảo không null
        String persistedContent = (type == MessageType.TEXT) ? content.trim() : "";
        message.setContent(persistedContent);
        message.setCreatedAt(java.util.Date.from(Instant.now()));
        message.setMessageType(type);
        message.setSticker(sticker);


        PrivateMessage saved = privateMessageRepository.save(message);
        PrivateMessageResponse response = MessageMapper.toPrivateMessageResponse(saved);

        // realtime

        return response;
    }

    //Lấy lịch sử hội thoại giữa hai người dùng, phân trang theo thời gian tăng dần.
    @Transactional(readOnly = true)
    public Page<PrivateMessageResponse> getPrivateMessageHistory(Long userAId, Long userBId, int page, int size) {
        Page<PrivateMessage> messages = privateMessageRepository.findConversation(userAId, userBId, PageRequest.of(page, size));
        return messages.map(MessageMapper::toPrivateMessageResponse);
    }

    // Lấy danh sách user từng nhắn và tin nhắn cuối cùng
    @Transactional(readOnly = true)
    public List<ConversationPreviewDTO> getLatestConversations(Long userId) {
        return privateMessageRepository.findLatestConversations(userId)
                .stream()
                .map(p -> MessageMapper.toConversationPreviewDTO(p, userId))
                .collect(Collectors.toList());
    }

    // Lấy danh sách tất cả bạn bè kèm thông tin conversation (nếu có)
    @Transactional(readOnly = true)
    public List<ConversationPreviewDTO> getFriendsWithConversations(Long userId) {
        // Lấy danh sách bạn bè
        User me = userService.getEntityById(userId);
        if (me == null) {
            return Collections.emptyList();
        }

        // Lấy danh sách conversations (người đã từng nhắn tin)
        List<ConversationPreviewDTO> conversations = getLatestConversations(userId);
        Map<Long, ConversationPreviewDTO> conversationMap = conversations.stream()
                .collect(Collectors.toMap(ConversationPreviewDTO::getOtherUserId, conv -> conv));

        // Lấy danh sách bạn bè từ FriendService
        com.ltm.memorygame.dto.friend.response.FriendListDTO friendList = 
            friendService.getAllForUser(userId);
        List<com.ltm.memorygame.dto.friend.response.FriendDTO> friends = 
            friendList.getFriends() != null ? friendList.getFriends() : Collections.emptyList();

        // Merge: tạo ConversationPreviewDTO cho mỗi bạn bè
        // Nếu đã có conversation, dùng thông tin từ conversation
        // Nếu chưa có, tạo mới với thông tin bạn bè và lastMessage = null
        List<ConversationPreviewDTO> result = new ArrayList<>();
        
        for (com.ltm.memorygame.dto.friend.response.FriendDTO friend : friends) {
            ConversationPreviewDTO preview = conversationMap.get(friend.getId());
            if (preview != null) {
                // Đã có conversation, dùng thông tin đã có
                result.add(preview);
            } else {
                // Chưa có conversation, tạo mới với thông tin bạn bè
                // Fetch username từ User entity
                User friendUser = userService.getEntityById(friend.getId());
                ConversationPreviewDTO newPreview = ConversationPreviewDTO.builder()
                        .otherUserId(friend.getId())
                        .otherUsername(friendUser != null ? friendUser.getUsername() : null)
                        .otherDisplayName(friend.getDisplayName())
                        .otherAvatarUrl(friend.getAvatarUrl())
                        .lastMessageId(null)
                        .lastMessageText(null)
                        .lastMessageType(null)
                        .lastStickerId(null)
                        .lastMessageTime(null)
                        .lastMessageFromSelf(false)
                        .build();
                result.add(newPreview);
            }
        }

        // Thêm các conversations với người không phải bạn bè vào cuối
        for (ConversationPreviewDTO conv : conversations) {
            boolean isFriend = friends.stream()
                    .anyMatch(f -> f.getId().equals(conv.getOtherUserId()));
            if (!isFriend) {
                result.add(conv);
            }
        }

        return result;
    }
}
