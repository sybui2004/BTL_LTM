package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.ThemeResponseDTO;
import com.ltm.memorygame.model.game.Theme;

import java.util.List;
import java.util.stream.Collectors;

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

    public static List<ThemeResponseDTO> toDTOList(List<Theme> themes) {
        return themes.stream().map(ThemeMapper::toDTO).collect(Collectors.toList());
    }
}


