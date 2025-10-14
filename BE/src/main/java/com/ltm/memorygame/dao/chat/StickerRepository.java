package com.ltm.memorygame.dao.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ltm.memorygame.model.chat.Sticker;

public interface StickerRepository extends JpaRepository<Sticker, Long> {
   
}
