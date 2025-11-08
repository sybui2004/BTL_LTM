package com.ltm.memorygame.config;

import com.ltm.memorygame.service.game.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThemeInitializer implements CommandLineRunner {

    private final ThemeService themeService;

    @Override
    public void run(String... args) throws Exception {
        // Initialize default themes on application startup
        themeService.initializeDefaultThemes();
    }
}
