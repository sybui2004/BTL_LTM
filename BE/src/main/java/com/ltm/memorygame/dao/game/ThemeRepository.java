package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.game.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
}
