package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dao.game.ThemeRepository;
import com.ltm.memorygame.dto.game.response.ThemeResponseDTO;
import com.ltm.memorygame.mapper.ThemeMapper;
import com.ltm.memorygame.model.game.Theme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThemeService {

    private final ThemeRepository themeRepository;

    @Transactional(readOnly = true)
    public List<ThemeResponseDTO> getAllThemes() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ThemeResponseDTO getThemeById(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Theme not found with id: " + id));
        return ThemeMapper.toDTO(theme);
    }

    @Transactional
    public ThemeResponseDTO createTheme(String name, String southPath, String assetPath) {
        Theme theme = new Theme();
        theme.setName(name);
        theme.setSouthPath(southPath);
        theme.setAssetPath(assetPath);
        
        Theme saved = themeRepository.save(theme);
        return ThemeMapper.toDTO(saved);
    }

    @Transactional
    public void initializeDefaultThemes() {
        // Check if themes already exist
        if (themeRepository.count() > 0) {
            System.out.println("[ThemeService] Themes already exist, skipping initialization");
            return;
        }

        System.out.println("[ThemeService] Initializing default themes...");

        // Christmas Theme
        Theme christmasTheme = new Theme();
        christmasTheme.setName("Christmas");
        christmasTheme.setSouthPath("/static/themes/Chirstmas");
        christmasTheme.setAssetPath("/static/themes/Chirstmas");
        themeRepository.save(christmasTheme);
        System.out.println("[ThemeService] Created Christmas theme");

        // Mid-Autumn Festival Theme
        Theme midAutumnTheme = new Theme();
        midAutumnTheme.setName("Mid-Autumn Festival");
        midAutumnTheme.setSouthPath("/static/themes/MidAutumnFestival");
        midAutumnTheme.setAssetPath("/static/themes/MidAutumnFestival");
        themeRepository.save(midAutumnTheme);
        System.out.println("[ThemeService] Created Mid-Autumn Festival theme");

        System.out.println("[ThemeService] Default themes initialized successfully");
    }
}
