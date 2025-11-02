package com.example.memorygame.controller.game;

import com.example.memorygame.model.game.CardDTO;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Memory card component with flip animation
 */
public class MemoryCard extends Button {
    private CardDTO cardData;
    private boolean isFlipped = false;
    private boolean isMatched = false;
    private ImageView cardImage;
    private String cardBackPath;
    private String cardFrontPath;
    
    public MemoryCard(CardDTO cardData, String cardBackPath) {
        this.cardData = cardData;
        this.cardBackPath = cardBackPath;
        // Check if imagePath already contains full URL, if not, prepend base URL
        String imagePath = cardData.getImagePath();
        if (imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"))) {
            this.cardFrontPath = imagePath;
        } else {
            this.cardFrontPath = "http://localhost:8080" + imagePath;
        }
        
        setupCard();
    }
    
    private void setupCard() {
        // Set card size
        setPrefSize(80, 100);
        setMinSize(80, 100);
        setMaxSize(80, 100);
        
        // Create image view
        cardImage = new ImageView();
        cardImage.setFitWidth(80);
        cardImage.setFitHeight(100);
        cardImage.setPreserveRatio(true);
        cardImage.setSmooth(true);
        
        // Load card back image
        loadCardBack();
        
        // Set button graphic
        setGraphic(cardImage);
        
        // Set button style
        getStyleClass().add("memory-card");
        
        // Note: Click handler will be set by GameScreenController
    }
    
    private void loadCardBack() {
        try {
            System.out.println("[MemoryCard] Loading card back: " + cardBackPath);
            Image backImage = new Image(cardBackPath);
            cardImage.setImage(backImage);
            System.out.println("[MemoryCard] Card back loaded successfully");
        } catch (Exception e) {
            System.err.println("[MemoryCard] Failed to load card back: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadCardFront() {
        try {
            System.out.println("[MemoryCard] Loading card front: " + cardFrontPath);
            Image frontImage = new Image(cardFrontPath);
            cardImage.setImage(frontImage);
            System.out.println("[MemoryCard] Card front loaded successfully");
        } catch (Exception e) {
            System.err.println("[MemoryCard] Failed to load card front: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void flipCard() {
        if (isMatched || isFlipped) {
            return; // Don't flip if already matched or flipped
        }
        
        System.out.println("[MemoryCard] Starting flip animation");
        
        // Create flip animation using ScaleX
        javafx.animation.ScaleTransition flipTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
        flipTransition.setFromX(1.0);
        flipTransition.setToX(0.0);
        flipTransition.setOnFinished(e -> {
            System.out.println("[MemoryCard] Halfway point reached, changing image");
            // Change image at halfway point
            if (!isFlipped) {
                loadCardFront();
                isFlipped = true;
                System.out.println("[MemoryCard] Flipped to front");
            } else {
                loadCardBack();
                isFlipped = false;
                System.out.println("[MemoryCard] Flipped to back");
            }
            
            // Complete the flip
            javafx.animation.ScaleTransition completeFlip = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
            completeFlip.setFromX(0.0);
            completeFlip.setToX(1.0);
            completeFlip.setOnFinished(e2 -> {
                System.out.println("[MemoryCard] Flip animation completed");
            });
            completeFlip.play();
        });
        
        flipTransition.play();
    }
    
    public void flipToFront() {
        if (isFlipped) return;
        
        javafx.animation.ScaleTransition flipTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
        flipTransition.setFromX(1.0);
        flipTransition.setToX(0.0);
        flipTransition.setOnFinished(e -> {
            loadCardFront();
            isFlipped = true;
            
            javafx.animation.ScaleTransition completeFlip = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
            completeFlip.setFromX(0.0);
            completeFlip.setToX(1.0);
            completeFlip.play();
        });
        
        flipTransition.play();
    }
    
    public void flipToBack() {
        if (!isFlipped) return;
        
        javafx.animation.ScaleTransition flipTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
        flipTransition.setFromX(1.0);
        flipTransition.setToX(0.0);
        flipTransition.setOnFinished(e -> {
            loadCardBack();
            isFlipped = false;
            
            javafx.animation.ScaleTransition completeFlip = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
            completeFlip.setFromX(0.0);
            completeFlip.setToX(1.0);
            completeFlip.play();
        });
        
        flipTransition.play();
    }
    
    public void markAsMatched() {
        isMatched = true;
        setDisable(true);
        
        // Add matched effect
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), this);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.8);
        scaleTransition.setToY(0.8);
        scaleTransition.play();
        
        getStyleClass().add("card-matched");
    }
    
    // Getters
    public CardDTO getCardData() {
        return cardData;
    }
    
    public boolean isFlipped() {
        return isFlipped;
    }
    
    public boolean isMatched() {
        return isMatched;
    }
    
    public String getImagePath() {
        return cardData.getImagePath();
    }
}
