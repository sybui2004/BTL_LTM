package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.friend.response.FriendDTO;
import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import com.ltm.memorygame.dto.user.response.UserProfileDTO;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.dto.user.response.UserSettingDTO;
import com.ltm.memorygame.model.enums.MatchStatus;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.model.user.UserSetting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserSettingDTO toUserSettingDTO(UserSetting setting) {
        if (setting == null)
            return null;

        return UserSettingDTO.builder()
                .musicVolume(setting.getMusicVolume())
                .soundFxVolume(setting.getSoundFxVolume())
                .notification(setting.isNotification())
                .language(setting.getLanguage())
                .build();
    }

    public MatchHistoryDTO toMatchHistoryDTO(User user, Match match) {
        User opponent = match.getOpponent(user);
        int userScore = match.getScoreFor(user);
        int opponentScore = match.getScoreFor(opponent);
        
        // Get rank points change from database (if available)
        Integer rankPointsChange = match.getRankPointsChangeFor(user);
        
        // Calculate rankPointsChange if not stored in DB
        if (rankPointsChange == null) {
            Long winnerId = match.getWinnerId();
            if (winnerId == null) {
                // Draw - both get 0
                rankPointsChange = 0;
            } else if (winnerId.equals(user.getId())) {
                // User won
                rankPointsChange = calculateWinnerRankPoints(userScore, opponentScore);
            } else {
                // User lost
                int loserPoints = calculateLoserRankPoints(opponentScore, userScore);
                rankPointsChange = -loserPoints;
            }
        }
        
        // Determine result based on rankPointsChange: positive = WIN, negative = LOSE, 0 = LOSE (no DRAW)
        String result;
        if (rankPointsChange > 0) {
            result = "WIN";
        } else {
            result = "LOSE";
        }

        return MatchHistoryDTO.builder()
                .matchId(match.getId())
                .opponentUsername(opponent.getUsername())
                .opponentDisplayName(opponent.getDisplayName())
                .userScore(userScore)
                .opponentScore(opponentScore)
                .result(result)
                .rankPointsChange(rankPointsChange)
                .playedAt(match.getStartTime())
                .build();
    }
    
    /**
     * Calculate winner rank points: [(winnerPairs + 2) / (loserPairs + 1)] * 10
     */
    private int calculateWinnerRankPoints(int winnerPairs, int loserPairs) {
        if (loserPairs < 0) loserPairs = 0;
        if (winnerPairs < 0) winnerPairs = 0;
        double points = ((double) (winnerPairs + 2)) / (loserPairs + 1);
        int result = (int) Math.floor(points * 10.0);
        return Math.max(0, result);
    }
    
    /**
     * Calculate loser rank points: ⌈(winnerPairs + 1) / (loserPairs + 1) - 1⌉ * 10
     */
    private int calculateLoserRankPoints(int winnerPairs, int loserPairs) {
        if (loserPairs < 0) loserPairs = 0;
        if (winnerPairs < 0) winnerPairs = 0;
        double result = ((double)(winnerPairs + 1) / (loserPairs + 1)) - 1;
        int points = (int) Math.ceil(result) * 10;
        return Math.max(0, points);
    }
    public UserResponseDTO toUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .status(user.getStatus())
                .score(user.getScore())
                .userSetting(toUserSettingDTO(user.getUserSetting()))
                .build();
    }

    public UserProfileDTO toUserProfileDTO(User user, List<Match> matches) {
        List<MatchHistoryDTO> matchHistoryDTO = matches.stream()
                .map(match -> toMatchHistoryDTO(user, match))
                .collect(Collectors.toList());

        return UserProfileDTO.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .score(user.getScore())
                .matchHistory(matchHistoryDTO)
                .build();
    }

    public FriendDTO toFriendDTO(User user) {
        if (user == null)
            return null;

        return FriendDTO.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .build();
    }

}
