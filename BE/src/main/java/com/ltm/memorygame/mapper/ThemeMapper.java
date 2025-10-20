package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.ThemeResponseDTO;
import com.ltm.memorygame.model.game.Theme;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ThemeMapper {

    public static ThemeResponseDTO toDTO(Theme theme) {
        if (theme == null) return null;
        return ThemeResponseDTO.builder()
                .id(theme.getId())
                .name(theme.getName())
                .southPath(theme.getSouthPath())
                .assetPath(theme.getAssetPath())
                .build();
    }
}


