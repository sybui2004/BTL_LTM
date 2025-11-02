package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.request.CreateRoomRequest;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;
import com.ltm.memorygame.dto.game.request.CreateMatchRequest;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.mapper.RoomMapper;
import com.ltm.memorygame.facade.RoomFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.ltm.memorygame.security.AuthUtils;

import java.util.List;
import com.ltm.memorygame.dto.game.request.RoomExitRequest;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomFacadeService roomFacadeService;

    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(request.getHostId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roomService.createRoom(request.getHostId(), request.getGuestId()));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getWaitingRooms());
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponseDTO> joinRoom(@Valid @RequestBody RoomExitRequest request) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(request.getPlayerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roomService.joinRoom(request.getRoomId(), request.getPlayerId()));
    }

    @PostMapping("/exit")
    public ResponseEntity<RoomResponseDTO> exitRoom(@Valid @RequestBody RoomExitRequest request) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(request.getPlayerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roomService.exitRoom(request.getRoomId(), request.getPlayerId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoom(id));
    }

    @PostMapping("/start")
    public ResponseEntity<MatchResponseDTO> startMatch(@Valid @RequestBody CreateMatchRequest request) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(request.getPlayer1Id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(roomFacadeService.startMatch(request));
    }

    /**
     * Danh sách phòng đang active (WAITING/READY/PLAYING) của user hiện tại.
     * FE dùng để biết mình đang ở phòng nào cho MatchChat.
     */
    @GetMapping("/my-active")
    public ResponseEntity<java.util.List<RoomResponseDTO>> getMyActiveRooms() {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var rooms = roomService.findRoomsByPlayer(authId);
        return ResponseEntity.ok(RoomMapper.toDTOList(rooms));
    }
}
