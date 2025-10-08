package com.ltm.memorygame.dao.chat;

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
}
