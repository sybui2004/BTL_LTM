package com.ltm.memorygame.service.game;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.model.game.Theme;
import com.ltm.memorygame.dao.game.ThemeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThemeService {
    private final ThemeRepository themeRepository;

    @Transactional(readOnly = true)
    public Theme getEntityById(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new NoSuchElementException("theme not found: " + themeId));
    }
}