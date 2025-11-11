package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.user.response.UserPresenceDTO;
import com.ltm.memorygame.model.user.User;

public final class PresenceMapper {

    private PresenceMapper() {}

    public static UserPresenceDTO toDTO(User u) {
        UserPresenceDTO DTO = new UserPresenceDTO();
        DTO.setUserId(u.getId());
        DTO.setDisplayName(u.getDisplayName());
        DTO.setUsername(u.getUsername());
        DTO.setAvatarUrl(u.getAvatarUrl());
        DTO.setStatus(u.getStatus().name());
        return DTO;
    }
}
