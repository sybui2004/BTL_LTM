package com.ltm.memorygame.dao.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.StickerType;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {
    List<Sticker> findByType(StickerType type);
}
