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

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class RoomFacadeService {

	private final RoomService roomService;
	private final MatchService matchService;

	@Transactional
	public MatchResponseDTO startMatch(CreateMatchRequest request) {
		Room room = roomService.getEntityById(request.getRoomId());

        if (room.getStatus() != RoomStatus.READY)
            throw new IllegalStateException("Room is not ready to start");

		// Only host can start; host must be player1
        if (!room.getHost().getId().equals(request.getPlayer1Id()))
            throw new IllegalStateException("Only the host can start the match");

		// player2 must be the room's guest
        if (room.getGuest() == null || !room.getGuest().getId().equals(request.getPlayer2Id()))
            throw new IllegalStateException("Guest player does not match the room's guest");

		room.setStatus(RoomStatus.PLAYING);
		roomService.updateAndMap(room);

		return matchService.createMatch(request);
	}
}


