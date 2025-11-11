package com.ltm.memorygame.dto.chat.response;

import com.ltm.memorygame.model.chat.StickerType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StickerResponse {
    private Long id;
    private String stickerPath;
    private StickerType type;
}
