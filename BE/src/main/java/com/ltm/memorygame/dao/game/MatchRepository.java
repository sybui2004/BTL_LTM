package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
