package com.ltm.memorygame.dao.chat;

import com.ltm.memorygame.model.chat.WorldMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldMessageRepository extends JpaRepository<WorldMessage, Long> {
}
