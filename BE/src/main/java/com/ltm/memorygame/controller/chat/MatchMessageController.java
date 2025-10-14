package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;
import com.ltm.memorygame.service.chat.MatchMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat/match")
public class MatchMessageController {

    private final MatchMessageService matchMessageService;
    // lấy lich sử chat trong trận đã được lưu vào RAM
    @GetMapping("/")
    public List<MatchMessageDTO> getMatchMessageHistory(@PathVariable String roomId) {
        return matchMessageService.getMatchMessageHistory(roomId);
    }
}
