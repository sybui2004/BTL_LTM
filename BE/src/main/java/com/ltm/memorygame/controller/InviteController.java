package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.request.InvitePlayerRequest;
import com.ltm.memorygame.dto.game.request.RoomActionRequest;
import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.service.room.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendInvite(@RequestBody InvitePlayerRequest request) {
        inviteService.sendInvite(request.getRoomId(), request.getSenderId(), request.getTargetId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept")
    public ResponseEntity<RoomResponseDTO> acceptInvite(@RequestBody RoomActionRequest request) {
        return ResponseEntity.ok(inviteService.acceptInvite(request.getRoomId(), request.getPlayerId()));
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectInvite(@RequestBody RoomActionRequest request) {
        inviteService.rejectInvite(request.getRoomId(), request.getPlayerId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending/{playerId}")
    public ResponseEntity<List<RoomInviteResponseDTO>> getPendingInvites(@PathVariable Long playerId) {
        return ResponseEntity.ok(inviteService.getPendingInvites(playerId));
    }
}
