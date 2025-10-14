package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.model.chat.Sticker;

public final class StickerMapper {
    private StickerMapper() {}

    public static StickerResponse toResponse(Sticker s) {
        if (s == null) return null;
        return StickerResponse.builder()
                .id(s.getId())
                .stickerPath(s.getSticker_path())
                .build();
    }
}
