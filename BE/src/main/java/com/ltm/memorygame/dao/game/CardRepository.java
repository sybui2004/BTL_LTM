package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Card;
import com.ltm.memorygame.model.game.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    
    List<Card> findByTheme(Theme theme);
    
    @Query("SELECT c FROM Card c WHERE c.theme.name = :themeName")
    List<Card> findByThemeName(@Param("themeName") String themeName);
}
