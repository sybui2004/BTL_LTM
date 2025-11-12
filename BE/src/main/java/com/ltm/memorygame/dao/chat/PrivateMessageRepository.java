package com.ltm.memorygame.dao.chat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ltm.memorygame.model.chat.PrivateMessage;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    // Lịch sử giữa hai user 
    @Query("""
           SELECT pm
           FROM PrivateMessage pm
           WHERE (pm.sender.id = :userA AND pm.receiver.id = :userB)
              OR (pm.sender.id = :userB AND pm.receiver.id = :userA)
           ORDER BY pm.createdAt ASC
           """)
    Page<PrivateMessage> findConversation(@Param("userA") Long userA,
                                          @Param("userB") Long userB,
                                          Pageable pageable);
   
   // Danh sách người dùng đã từng nhắn + tin nhắn cuối cùng với userId
    @Query(value = """
        SELECT 
            pm.id                            AS lastMessageId,
            pm.content                       AS lastMessageText,
            pm.message_type                  AS lastMessageType,
            pm.sticker_id                    AS lastStickerId,
            pm.created_at                    AS lastMessageTime,
            pm.sender_id                     AS senderId,
            pm.receiver_id                   AS receiverId,
            other_u.id                       AS otherUserId,
            other_u.username                 AS otherUsername,
            other_u.display_name             AS otherDisplayName,
            other_u.avatar_url               AS otherAvatarUrl
        FROM (
            SELECT p.*,
                   CASE WHEN p.sender_id = :userId THEN p.receiver_id ELSE p.sender_id END AS peer_id,
                   ROW_NUMBER() OVER (
                       PARTITION BY CASE WHEN p.sender_id = :userId THEN p.receiver_id ELSE p.sender_id END
                       ORDER BY p.created_at DESC, p.id DESC
                   ) AS rn
            FROM private_message p
            WHERE p.sender_id = :userId OR p.receiver_id = :userId
        ) pm
        JOIN users other_u ON other_u.id = pm.peer_id
        WHERE pm.rn = 1
        ORDER BY pm.created_at DESC, pm.id DESC
        """, nativeQuery = true)
    List<ConversationPreviewProjection> findLatestConversations(@Param("userId") Long userId);
}
