package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<WorldMessageResponse> getWorldHistory() {
        return worldMessageService.getRecentMessages();
    }

}
