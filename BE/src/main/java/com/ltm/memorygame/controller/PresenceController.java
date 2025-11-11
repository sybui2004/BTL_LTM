package com.ltm.memorygame.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dto.user.response.UserPresenceDTO;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.service.user.PresenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    // Update trạng thái của user
    @PutMapping("/{userId}")
    public ResponseEntity<UserPresenceDTO> setStatus(
            @PathVariable Long userId,
            @RequestParam("status") UserStatus status
    ) {
        return ResponseEntity.ok(presenceService.setStatus(userId, status));
    }

}
