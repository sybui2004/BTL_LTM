package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.ltm.memorygame.model.user.User;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findTop20ByPlayer1OrPlayer2OrderByStartTimeDesc(User player1, User player2);

    List<Match> findByPlayer1OrPlayer2(User player1, User player2);
    
    Optional<Match> findByRoomAndStatus(Room room, MatchStatus status);
    
    boolean existsByRoom(Room room);
}
