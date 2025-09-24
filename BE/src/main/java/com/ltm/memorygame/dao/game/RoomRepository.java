package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
