package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.request.InvitePlayerRequest;
import com.ltm.memorygame.dto.game.request.RoomActionRequest;
import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.facade.InviteFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteFacadeService inviteFacadeService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendInvite(@Valid @RequestBody InvitePlayerRequest request) {
        inviteFacadeService.sendInvite(request.getRoomId(), request.getSenderId(), request.getTargetId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept")
    public ResponseEntity<RoomResponseDTO> acceptInvite(@Valid @RequestBody RoomActionRequest request) {
        return ResponseEntity.ok(inviteFacadeService.acceptInvite(request.getRoomId(), request.getPlayerId()));
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectInvite(@Valid @RequestBody RoomActionRequest request) {
        inviteFacadeService.rejectInvite(request.getRoomId(), request.getPlayerId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending/{playerId}")
    public ResponseEntity<List<RoomInviteResponseDTO>> getPendingInvites(@PathVariable Long playerId) {
        return ResponseEntity.ok(inviteFacadeService.getPendingInvites(playerId));
    }
}
