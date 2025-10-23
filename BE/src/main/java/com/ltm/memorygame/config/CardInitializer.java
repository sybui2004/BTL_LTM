package com.ltm.memorygame.config;

import com.ltm.memorygame.dao.game.CardRepository;
import com.ltm.memorygame.dao.game.ThemeRepository;
import com.ltm.memorygame.model.game.Card;
import com.ltm.memorygame.model.game.Theme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CardInitializer implements CommandLineRunner {
    
    private final CardRepository cardRepository;
    private final ThemeRepository themeRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultCards();
    }
    
    private void initializeDefaultCards() {
        List<Theme> themes = themeRepository.findAll();
        if (themes.isEmpty()) {
            System.out.println("[CardInitializer] No themes found, skipping card initialization.");
            return;
        }
        
        // Check if cards exist and have correct count
        long totalCards = cardRepository.count();
        long expectedCards = themes.size() * 25; // 25 cards per theme
        
        if (totalCards == expectedCards) {
            System.out.println("[CardInitializer] Cards already exist with correct count (" + totalCards + "), skipping initialization.");
            return;
        }
        
        if (totalCards > 0) {
            System.out.println("[CardInitializer] Cards exist but count is incorrect (" + totalCards + " vs " + expectedCards + "), recreating...");
            cardRepository.deleteAll();
        }
        
        for (Theme theme : themes) {
            createCardsForTheme(theme);
        }
        
        System.out.println("[CardInitializer] Initialized " + cardRepository.count() + " default cards.");
    }
    
    private void createCardsForTheme(Theme theme) {
        String themeName = theme.getName();
        String assetPath = theme.getAssetPath();
        
        if ("Christmas".equals(themeName)) {
            createChristmasCards(theme, assetPath);
        } else if ("Mid-Autumn Festival".equals(themeName)) {
            createMidAutumnCards(theme, assetPath);
        }
    }
    
    private void createChristmasCards(Theme theme, String assetPath) {
        // Create 25 unique cards for Christmas theme (card_01.png to card_25.png)
        for (int i = 1; i <= 25; i++) {
            Card card = new Card();
            card.setTheme(theme);
            card.setCardPath(assetPath + "/card_" + String.format("%02d", i) + ".png");
            cardRepository.save(card);
        }
        System.out.println("[CardInitializer] Created 25 Christmas cards");
    }
    
    private void createMidAutumnCards(Theme theme, String assetPath) {
        // Create 25 unique cards for Mid-Autumn Festival theme (card_01.png to card_25.png)
        for (int i = 1; i <= 25; i++) {
            Card card = new Card();
            card.setTheme(theme);
            card.setCardPath(assetPath + "/card_" + String.format("%02d", i) + ".png");
            cardRepository.save(card);
        }
        System.out.println("[CardInitializer] Created 25 Mid-Autumn Festival cards");
    }
}
