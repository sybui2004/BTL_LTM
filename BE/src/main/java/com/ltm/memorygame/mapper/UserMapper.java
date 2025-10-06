package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import com.ltm.memorygame.dto.user.response.UserProfileDTO;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.dto.user.response.UserSettingDTO;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.model.user.UserSetting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    // Map UserSetting -> UserSettingDTO
    public UserSettingDTO toUserSettingDTO(UserSetting setting) {
        if (setting == null) return null;

        return UserSettingDTO.builder()
                .musicVolume(setting.getMusicVolume())
                .soundFxVolume(setting.getSoundFxVolume())
                .notification(setting.isNotification())
                .language(setting.getLanguage())
                .build();
    }

    // Map Match -> MatchHistoryDTO
    public MatchHistoryDTO toMatchHistoryDTO(User user, Match match) {
        return MatchHistoryDTO.builder()
                .matchId(match.getId())
                .opponentUsername(match.getOpponent(user).getUsername())
                .userScore(match.getScoreFor(user))
                .opponentScore(match.getScoreFor(match.getOpponent(user)))
                .result(match.getResultFor(user).name())
                .playedAt(match.getStartTime())
                .build();
    }

    // Map User -> UserResponseDTO
    public UserResponseDTO toUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .userSetting(toUserSettingDTO(user.getUserSetting()))
                .build();
    }

    // Map UserSetting -> UserProfileDTO
    public UserProfileDTO toUserProfileDTO(User user, List<Match> matches) {
        List<MatchHistoryDTO> matchHistoryDTO = matches.stream()
                .map(match -> toMatchHistoryDTO(user, match))
                .collect(Collectors.toList());

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .userSetting(toUserSettingDTO(user.getUserSetting()))
                .matchHistory(matchHistoryDTO)
                .build();
    }

}
