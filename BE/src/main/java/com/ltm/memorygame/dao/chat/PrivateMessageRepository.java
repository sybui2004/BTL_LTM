package com.ltm.memorygame.dao.chat;

import com.ltm.memorygame.model.chat.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
}
