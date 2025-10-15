package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.request.CreateMatchRequest;
import com.ltm.memorygame.dto.game.request.FinishedMatchRequest;
import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;
import com.ltm.memorygame.service.game.MatchService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor

public class MatchController {
    private final MatchService matchService;

    @PutMapping("/{matchId}/finish")
    public ResponseEntity<MatchResponseDTO> finishMatch(@PathVariable Long matchId, @RequestBody FinishedMatchRequest request) {
        return ResponseEntity.ok(matchService.finishMatch(matchId, request));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<MatchHistoryDTO>> getMatchHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(matchService.getMatchHistory(userId));
    }

}
