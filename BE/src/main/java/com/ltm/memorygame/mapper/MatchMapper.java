package com.ltm.memorygame.mapper;
import org.springframework.stereotype.Component;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;

@Component
public class MatchMapper {
    public MatchResponseDTO toMatchResponseDTO(Match match){
        return MatchResponseDTO.builder()
                .id(match.getId())
                .roomId(match.getRoom().getId())
                .player1Score(match.getPlayer1Score())
                .player2Score(match.getPlayer2Score())
                .themeId(match.getTheme().getId())
                .boardSize(match.getBoardSize())
                .timePerMove(match.getTimePerMove())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .status(match.getStatus())
                .build();
    }
}