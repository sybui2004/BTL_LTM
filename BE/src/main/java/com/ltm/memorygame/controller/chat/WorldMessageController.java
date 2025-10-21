package com.ltm.memorygame.controller.chat;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.service.chat.WorldMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/world")
public class WorldMessageController {

    private final WorldMessageService worldMessageService;

    //Lấy lịch sử 100 tin nhắn của kênh thế giới
    @GetMapping("/")
    public Page<WorldMessageResponse> getWorldHistory(
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        int p = Math.max(0, page);
        int s = (size == null || size <= 0 || size > 100) ? 20 : size;
        return worldMessageService.getRecentMessages(p, s);
    }

}
