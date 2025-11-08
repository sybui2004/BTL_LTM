package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.service.chat.StickerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stickers")
public class StickerController {

    private final StickerService stickerService;

    @GetMapping
    public ResponseEntity<List<StickerResponse>> getAllStickers() {
        return ResponseEntity.ok(stickerService.getAllStickers());
    }
}

