package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.StickerType;
import com.ltm.memorygame.service.chat.StickerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stickers")
public class StickerController {

    private final StickerService stickerService;

    @GetMapping
    public ResponseEntity<List<Sticker>> getStickers(@RequestParam(name = "type", defaultValue = "NORMAL") String type) {
        try {
            StickerType stickerType = StickerType.valueOf(type.toUpperCase());
            List<Sticker> stickers = stickerService.getStickersByType(stickerType);
            return ResponseEntity.ok(stickers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Invalid type
        }
    }
}

