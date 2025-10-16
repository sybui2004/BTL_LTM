package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.response.ThemeResponseDTO;
import com.ltm.memorygame.mapper.ThemeMapper;
import com.ltm.memorygame.service.game.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping
    public ResponseEntity<List<ThemeResponseDTO>> getAllThemes() {
        return ResponseEntity.ok(ThemeMapper.toDTOList(themeService.getAll()));
    }
}


