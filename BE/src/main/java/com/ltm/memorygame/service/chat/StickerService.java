package com.ltm.memorygame.service.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.mapper.StickerMapper;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.StickerType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StickerService {

    private final StickerRepository stickerRepository;

    @Transactional(readOnly = true)
    public List<StickerResponse> getAllStickers() {
        return stickerRepository.findAll().stream()
                .map(StickerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Sticker> getStickersByType(StickerType type) {
        return stickerRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public StickerResponse getStickerById(Long stickerId) {
        if (stickerId == null) {
            return null;
        }
        return stickerRepository.findById(stickerId)
                .map(StickerMapper::toResponse)
                .orElse(null);
    }
}

