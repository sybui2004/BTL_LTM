package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dao.game.MatchRepository;
import com.ltm.memorygame.dao.game.RoomRepository;
import com.ltm.memorygame.dao.game.ThemeRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.game.request.CreateMatchRequest;
import com.ltm.memorygame.dto.game.request.FinishedMatchRequest;
import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;
import com.ltm.memorygame.mapper.MatchMapper;
import com.ltm.memorygame.mapper.UserMapper;
import com.ltm.memorygame.model.enums.MatchStatus;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.game.Theme;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ThemeRepository themeRepository;
    private final UserMapper userMapper;
    private final MatchMapper matchMapper;

    @Transactional(readOnly = true)
    public List<MatchHistoryDTO> getMatchHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Match> matches = matchRepository.findTop20ByPlayer1OrPlayer2OrderByStartTimeDesc(user, user);

        return matches.stream()
                .map(match -> userMapper.toMatchHistoryDTO(user, match))
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchResponseDTO createMatch(CreateMatchRequest request){
        User player1 = userRepository.findById(request.getPlayer1Id())
                .orElseThrow(() -> new NoSuchElementException("Player 1 not found"));
        User player2 = userRepository.findById(request.getPlayer2Id())
                .orElseThrow(() -> new NoSuchElementException("Player 2 not found"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("Room not found"));
        Theme theme = themeRepository.findById(request.getThemeId())
                .orElseThrow(() -> new NoSuchElementException("Theme not found"));

        Match match = new Match();
        match.setBoardSize(request.getBoardSize());
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setRoom(room);
        match.setTheme(theme);
        match.setTimePerMove(request.getTimePerMove());
        match.setStartTime(new Date());
        match.setStatus(MatchStatus.PLAYING);

        Match savedMatch = matchRepository.save(match);
        return matchMapper.toMatchResponseDTO(savedMatch);
    }

    @Transactional
    public MatchResponseDTO finishMatch(Long matchId, FinishedMatchRequest request) {
        Match match = getEntityById(matchId);

        match.setPlayer1Score(request.getPlayer1Score());
        match.setPlayer2Score(request.getPlayer2Score());
        match.setEndTime(new Date());
        
        Long hostId = match.getRoom().getHost().getId();
        boolean hostIsPlayer1 = match.getPlayer1().getId().equals(hostId);
        int hostScore = hostIsPlayer1 ? request.getPlayer1Score() : request.getPlayer2Score();
        int opponentScore = hostIsPlayer1 ? request.getPlayer2Score() : request.getPlayer1Score();
        if (hostScore > opponentScore) {
            match.setStatus(MatchStatus.WIN);
        } else if (hostScore < opponentScore) {
            match.setStatus(MatchStatus.LOSE);
        } else {
            match.setStatus(MatchStatus.LOSE);
        }

        Match updatedMatch = matchRepository.save(match);
        return matchMapper.toMatchResponseDTO(updatedMatch);
    }

    @Transactional(readOnly = true)
    public Match getEntityById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Match not found"));
    }
}
