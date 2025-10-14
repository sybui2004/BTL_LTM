package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.game.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByHostIdOrGuestIdAndStatusIn(Long hostId, Long guestId, List<RoomStatus> statuses);
    List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);
}
