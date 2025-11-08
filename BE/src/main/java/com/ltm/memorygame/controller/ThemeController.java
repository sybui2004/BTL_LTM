package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.response.ThemeResponseDTO;
import com.ltm.memorygame.service.game.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping
    public ResponseEntity<List<ThemeResponseDTO>> getAllThemes() {
        return ResponseEntity.ok(themeService.getAllThemes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThemeResponseDTO> getThemeById(@PathVariable Long id) {
        return ResponseEntity.ok(themeService.getThemeById(id));
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeThemes() {
        themeService.initializeDefaultThemes();
        return ResponseEntity.ok("Themes initialized successfully");
    }
}
