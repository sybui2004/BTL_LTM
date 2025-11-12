package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import com.ltm.memorygame.service.game.CardService;
import com.ltm.memorygame.service.game.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    
    private final CardService cardService;
    private final GameSessionService gameSessionService;
    
    @GetMapping
    public ResponseEntity<List<CardResponseDTO>> getAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDTO> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }
    
    @GetMapping("/game")
    public ResponseEntity<List<CardResponseDTO>> getCardsForGame(
            @RequestParam String theme,
            @RequestParam String size,
            @RequestParam Long roomId) {
        return ResponseEntity.ok(gameSessionService.getCardsForGameSession(roomId, theme, size));
    }
}
