package com.ltm.memorygame.service.match;

import com.ltm.memorygame.dao.game.MatchRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import com.ltm.memorygame.mapper.UserMapper;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<MatchHistoryDTO> getMatchHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = matchRepository.findTop20ByPlayer1OrPlayer2OrderByStartTimeDesc(user, user);

        return matches.stream()
                .map(match -> userMapper.toMatchHistoryDTO(user, match))
                .collect(Collectors.toList());
    }
}
