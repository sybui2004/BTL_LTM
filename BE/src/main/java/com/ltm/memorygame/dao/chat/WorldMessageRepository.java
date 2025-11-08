package com.ltm.memorygame.dao.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ltm.memorygame.model.chat.WorldMessage;

@Repository
public interface WorldMessageRepository extends JpaRepository<WorldMessage, Long> {

    // Lấy danh sách 100 tin nhắn global mới nhất (sắp xếp theo created_at DESC)
    @Query(value = "SELECT * FROM world_message ORDER BY created_at DESC LIMIT 100", nativeQuery = true)
    List<WorldMessage> findTop100ByOrderByCreatedAtDesc();

}
