package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}
