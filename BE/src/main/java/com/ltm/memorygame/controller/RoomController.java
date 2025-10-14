package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.request.CreateRoomRequest;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.service.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.ltm.memorygame.dto.game.request.RoomExitRequest;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request.getHostId(), request.getGuestId()));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getWaitingRooms());
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponseDTO> joinRoom(@RequestBody RoomExitRequest request) {
        return ResponseEntity.ok(roomService.joinRoom(request.getRoomId(), request.getPlayerId()));
    }

    @PostMapping("/exit")
    public ResponseEntity<RoomResponseDTO> exitRoom(@RequestBody RoomExitRequest request) {
        return ResponseEntity.ok(roomService.exitRoom(request.getRoomId(), request.getPlayerId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoom(id));
    }
}
