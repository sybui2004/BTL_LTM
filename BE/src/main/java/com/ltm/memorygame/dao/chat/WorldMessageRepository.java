package com.ltm.memorygame.dao.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ltm.memorygame.model.chat.WorldMessage;

@Repository
public interface WorldMessageRepository extends JpaRepository<WorldMessage, Long> {

    // Lấy danh sách tin nhắn global mới nhất (desc), truyền limit bằng Pageable
    Page<WorldMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
