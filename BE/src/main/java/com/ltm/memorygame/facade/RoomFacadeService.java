package com.ltm.memorygame.facade;

import com.ltm.memorygame.dto.game.request.CreateMatchRequest;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.service.game.MatchService;
import com.ltm.memorygame.service.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoomFacadeService {

	private final RoomService roomService;
	private final MatchService matchService;

	@Transactional
	public MatchResponseDTO startMatch(CreateMatchRequest request) {
		Room room = roomService.getEntityById(request.getRoomId());
		
		System.out.println("[RoomFacade] Starting match for room " + request.getRoomId() + 
			" - Current status: " + room.getStatus() + 
			", Host: " + (room.getHost() != null ? room.getHost().getId() : "null") +
			", Guest: " + (room.getGuest() != null ? room.getGuest().getId() : "null"));

        if (room.getStatus() != RoomStatus.READY) {
            System.err.println("[RoomFacade] Room " + request.getRoomId() + " is not READY, current status: " + room.getStatus());
            throw new IllegalStateException("Room is not ready to start");
        }

		// Only host can start; host must be player1
        if (room.getHost() == null || !room.getHost().getId().equals(request.getPlayer1Id())) {
            System.err.println("[RoomFacade] Host mismatch - Room host: " + 
                (room.getHost() != null ? room.getHost().getId() : "null") + 
                ", Request player1: " + request.getPlayer1Id());
            throw new IllegalStateException("Only the host can start the match");
        }

		// player2 must be the room's guest
        if (room.getGuest() == null || !room.getGuest().getId().equals(request.getPlayer2Id())) {
            System.err.println("[RoomFacade] Guest mismatch - Room guest: " + 
                (room.getGuest() != null ? room.getGuest().getId() : "null") + 
                ", Request player2: " + request.getPlayer2Id());
            throw new IllegalStateException("Guest player does not match the room's guest");
        }

		room.setStatus(RoomStatus.PLAYING);
		roomService.updateAndMap(room);
		System.out.println("[RoomFacade] Room " + request.getRoomId() + " set to PLAYING");

		return matchService.createMatch(request);
	}
}


