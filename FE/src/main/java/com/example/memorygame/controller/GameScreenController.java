package com.example.memorygame.controller;

import com.example.memorygame.model.game.GameSettings;
import com.example.memorygame.model.game.ThemeDTO;
import com.example.memorygame.model.game.CardDTO;
import com.example.memorygame.controller.game.MemoryCard;
import com.example.memorygame.utils.CardApi;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.view.GameScreen;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Game Screen
 */
public class GameScreenController {
    
    // Game settings
    private GameSettings gameSettings;
    private static GameSettings staticGameSettings;
    
    // Game state
    private List<MemoryCard> cards = new ArrayList<>();
    private List<CardDTO> cardData = new ArrayList<>();
    private boolean isSendingMessage = false; // Flag to prevent double-flip
    
    // Turn management
    private boolean isMyTurn = true; // Start with host's turn
    private int player1Score = 0;
    private int player2Score = 0;
    private MemoryCard firstFlippedCard = null;
    private MemoryCard secondFlippedCard = null;
    private int flippedCardsCount = 0;
    private boolean isResolving = false; // Block input while waiting for result/animations
    
    // Timer
    private Timeline turnTimer;
    private int currentTurnTime;
    private int initialTurnTime; // From game settings
    
    // UI Components
    @FXML private StackPane gameContainer;
    @FXML private GridPane cardGrid;
    @FXML private ImageView backgroundImage;
    @FXML private StackPane backButton;
    @FXML private HBox myPanel;
    @FXML private HBox opponentPanel;
    @FXML private Label myNameLabel;
    @FXML private Label opponentNameLabel;
    @FXML private ImageView myAvatar;
    @FXML private ImageView opponentAvatar;
    @FXML private Label myScoreLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private TextField myChatField;
    @FXML private TextField opponentChatField;
    @FXML private Label timerLabel;
    
    private GameScreen screen;
    
    public GameScreenController() {
        this.gameSettings = staticGameSettings;
        
        if (gameSettings != null) {
            isMyTurn = gameSettings.isHost();
            initialTurnTime = parseTimeSetting(gameSettings.getTime());
        } else {
            initialTurnTime = 30; // Default if no settings
        }
        currentTurnTime = initialTurnTime;
        
        setupTCPHandlers();
    }
    
    public static void setGameSettings(GameSettings settings) {
        staticGameSettings = settings;
    }
    
    public GameScreen getScreen() {
        if (screen == null) {
            this.screen = new GameScreen();
        }
        return screen;
    }
    
    @FXML
    private void initialize() {
        System.out.println("[GameScreen] Initializing with settings: " + gameSettings);
        setupGame();
        setupResizeListener();
        setupOverlayUI();
        startTurnTimer();
    }
    
    private void setupGame() {
        if (gameSettings == null) {
            System.err.println("[GameScreen] ERROR: Game settings are null!");
            return;
        }
        
        setupThemeBackground();
        setupCardGrid();
        
        System.out.println("[GameScreen] Game setup completed");
    }
    
    private void setupOverlayUI() {
        if (backButton != null) {
            backButton.setOnMouseClicked(e -> handleBack());
        }
        
        // Load user names based on host/guest role
        if (gameSettings != null) {
            if (gameSettings.isHost()) {
                // Host: myName = player1Name, opponentName = player2Name
                if (myNameLabel != null) {
                    myNameLabel.setText(gameSettings.getPlayer1Name());
                }
                if (opponentNameLabel != null) {
                    opponentNameLabel.setText(gameSettings.getPlayer2Name());
                }
            } else {
                // Guest: myName = player2Name, opponentName = player1Name
                if (myNameLabel != null) {
                    myNameLabel.setText(gameSettings.getPlayer2Name());
                }
                if (opponentNameLabel != null) {
                    opponentNameLabel.setText(gameSettings.getPlayer1Name());
                }
            }
        } else {
            // Fallback
            if (myNameLabel != null) {
                myNameLabel.setText("Me");
            }
            if (opponentNameLabel != null) {
                opponentNameLabel.setText("Opponent");
            }
        }
        
        // Load default avatars (can be replaced with real URLs later)
        String defaultAvatar = "http://localhost:8080/static/avatars/default_avatar.png";
        try {
            if (myAvatar != null) {
                myAvatar.setImage(new Image(defaultAvatar));
            }
            if (opponentAvatar != null) {
                opponentAvatar.setImage(new Image(defaultAvatar));
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load avatars: " + e.getMessage());
        }
        
        // Set initial scores based on host/guest role
        if (gameSettings != null) {
            if (gameSettings.isHost()) {
                // Host: myScore = player1Score, opponentScore = player2Score
                if (myScoreLabel != null) {
                    myScoreLabel.setText(String.valueOf(player1Score));
                }
                if (opponentScoreLabel != null) {
                    opponentScoreLabel.setText(String.valueOf(player2Score));
                }
            } else {
                // Guest: myScore = player2Score, opponentScore = player1Score
                if (myScoreLabel != null) {
                    myScoreLabel.setText(String.valueOf(player2Score));
                }
                if (opponentScoreLabel != null) {
                    opponentScoreLabel.setText(String.valueOf(player1Score));
                }
            }
        } else {
            // Fallback
            if (myScoreLabel != null) {
                myScoreLabel.setText("0");
            }
            if (opponentScoreLabel != null) {
                opponentScoreLabel.setText("0");
            }
        }
    }
    
    private void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            
            // Navigate back to RoomScreen
            RoomScreenController roomController = new RoomScreenController();
            Scene roomScene = new Scene(roomController.getScreen().getRoot());
            roomScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/RoomScreenStyle.css").toExternalForm());
            
            stage.setScene(roomScene);
            stage.setTitle("Memory Game - Room");
            stage.show();
            
            System.out.println("[GameScreen] Navigated back to room screen");
            
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to navigate back: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupThemeBackground() {
        ThemeDTO theme = gameSettings.getTheme();
        if (theme == null || theme.assetPath == null) {
            System.err.println("[GameScreen] No theme or asset path found");
            loadFallbackBackground();
            return;
        }
        
        loadBackgroundImage(theme.assetPath);
    }
    
    private void loadBackgroundImage(String assetPath) {
        String backgroundUrl = "http://localhost:8080" + assetPath + "/background.jpg";
        System.out.println("[GameScreen] Loading background from: " + backgroundUrl);
        
        try {
            Image bgImage = new Image(backgroundUrl);
            if (bgImage.isError()) {
                System.err.println("[GameScreen] Background image failed to load");
                loadFallbackBackground();
            } else {
                backgroundImage.setImage(bgImage);
                backgroundImage.setPreserveRatio(false);
                System.out.println("[GameScreen] Background loaded successfully");
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Error loading background: " + e.getMessage());
            loadFallbackBackground();
        }
    }
    
    private void loadFallbackBackground() {
        try {
            String fallbackUrl = getClass().getResource("/com/example/memorygame/assets/images/default_background.png").toExternalForm();
            Image fallbackImage = new Image(fallbackUrl);
            backgroundImage.setImage(fallbackImage);
            backgroundImage.setPreserveRatio(false);
            System.out.println("[GameScreen] Fallback background loaded");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load fallback background: " + e.getMessage());
        }
    }
    
    private void setupResizeListener() {
        if (backgroundImage != null && gameContainer != null) {
            gameContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
                updateBackgroundSize();
            });
            gameContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                updateBackgroundSize();
            });
            updateBackgroundSize();
        }
    }
    
    private void updateBackgroundSize() {
        if (backgroundImage != null && gameContainer != null) {
            backgroundImage.setFitWidth(gameContainer.getWidth());
            backgroundImage.setFitHeight(gameContainer.getHeight());
        }
    }
    
    private void setupCardGrid() {
        String[] sizeParts = gameSettings.getSize().split("x");
        int rows = Integer.parseInt(sizeParts[0]);
        int cols = Integer.parseInt(sizeParts[1]);
        
        cardGrid.getChildren().clear();
        
        createCards(rows, cols);
        
        System.out.println("[GameScreen] Card grid setup completed - " + rows + "x" + cols);
    }
    
    private void createCards(int rows, int cols) {
        System.out.println("[GameScreen] Creating cards for grid: " + rows + "x" + cols);
        
        loadCardsFromServer(rows, cols);
    }
    
    private void loadCardsFromServer(int rows, int cols) {
        try {
            System.out.println("[GameScreen] Loading cards from server - Theme: " + gameSettings.getTheme().name + 
                             ", Size: " + gameSettings.getSize() + ", RoomId: " + gameSettings.getRoomId());
            
            List<CardDTO> fetchedCards = CardApi.getCardsForGame(
                gameSettings.getTheme().name, 
                gameSettings.getSize(),
                gameSettings.getRoomId()
            );
            
            if (fetchedCards != null && !fetchedCards.isEmpty()) {
                System.out.println("[GameScreen] Successfully loaded " + fetchedCards.size() + " cards from server");
                cardData = fetchedCards;
                createMemoryCards(rows, cols);
            } else {
                System.err.println("[GameScreen] No cards returned from server, using mock cards");
                createMockCards(rows, cols);
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load cards from server: " + e.getMessage());
            e.printStackTrace();
            createMockCards(rows, cols);
        }
    }
    
    private void createMemoryCards(int rows, int cols) {
        cardGrid.getChildren().clear();
        cards.clear();
        String cardBackPath = "http://localhost:8080" + gameSettings.getTheme().assetPath + "/card_back.png";
        
        for (int i = 0; i < cardData.size(); i++) {
            CardDTO cardDTO = cardData.get(i);
            MemoryCard memoryCard = new MemoryCard(cardDTO, cardBackPath);
            
            final int cardIndex = i;
            final int finalRow = i / cols;
            final int finalCol = i % cols;
            
            memoryCard.setOnAction(e -> {
                System.out.println("[GameScreen] Card clicked at position: " + finalRow + "," + finalCol);
                
                if (isResolving || !isMyTurn) { // Block input if resolving or not my turn
                    System.out.println("[GameScreen] Ignoring click - isResolving: " + isResolving + ", isMyTurn: " + isMyTurn);
                    return;
                }
                
                if (memoryCard.isFlipped() || memoryCard.isMatched()) {
                    System.out.println("[GameScreen] Card already flipped or matched, ignoring click");
                    return;
                }
                
                if (flippedCardsCount >= 2) {
                    System.out.println("[GameScreen] Already have 2 cards flipped, ignoring click");
                    return;
                }
                
                memoryCard.flipToFront();
                flippedCardsCount++;
                System.out.println("[GameScreen] Flipped card locally at position: " + finalRow + "," + finalCol + " (flipped: " + flippedCardsCount + ")");
                
                if (firstFlippedCard == null) {
                    firstFlippedCard = memoryCard;
                    System.out.println("[GameScreen] Set firstFlippedCard at position: " + finalRow + "," + finalCol);
                } else {
                    secondFlippedCard = memoryCard;
                    System.out.println("[GameScreen] Set secondFlippedCard at position: " + finalRow + "," + finalCol + " - sending to server for match check");
                    isResolving = true; // Lock input while waiting for server result
                    sendCardsForMatchCheck();
                }
                
                sendCardFlippedMessage(cardIndex, finalRow, finalCol);
            });
            
            cards.add(memoryCard);
            cardGrid.add(memoryCard, finalCol, finalRow);
        }
        System.out.println("[GameScreen] Created " + cards.size() + " memory cards");
    }
    
    private void createMockCards(int rows, int cols) {
        cardGrid.getChildren().clear();
        cards.clear();
        cardData.clear();
        
        int totalCards = rows * cols;
        String assetPath = gameSettings.getTheme().assetPath;
        String cardBackPath = "http://localhost:8080" + assetPath + "/card_back.png";
        
        for (int i = 0; i < totalCards; i++) {
            int cardNumber = (i % (totalCards / 2)) + 1;
            String imagePath = "http://localhost:8080" + assetPath + "/card_" + String.format("%02d", cardNumber) + ".png";
            
            CardDTO mockCard = new CardDTO();
            mockCard.setId((long) i);
            mockCard.setImagePath(imagePath);
            
            cardData.add(mockCard);
            
            MemoryCard memoryCard = new MemoryCard(mockCard, cardBackPath);
            
            final int finalRow = i / cols;
            final int finalCol = i % cols;
            
            memoryCard.setOnAction(e -> {
                if (!memoryCard.isFlipped() && !memoryCard.isMatched()) {
                    memoryCard.flipCard();
                }
            });
            
            cards.add(memoryCard);
            cardGrid.add(memoryCard, finalCol, finalRow);
        }
        
        System.out.println("[GameScreen] Created " + totalCards + " mock cards");
    }
    
    private void setupTCPHandlers() {
        try {
            TCPClient client = TCPClient.getInstance();
            
            client.onMessage("CARD_FLIPPED", message -> {
                System.out.println("[GameScreen] Received CARD_FLIPPED message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Integer cardIndex = convertToInteger(data.get("cardIndex"));
                    Integer row = convertToInteger(data.get("row"));
                    Integer col = convertToInteger(data.get("col"));
                    
                    if (cardIndex != null && row != null && col != null) {
                        Platform.runLater(() -> {
                            flipCardAtPosition(cardIndex, row, col);
                        });
                    }
                }
            });
            
            client.onMessage("CARD_MATCHED", message -> {
                System.out.println("[GameScreen] Received CARD_MATCHED message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Integer cardIndex1 = convertToInteger(data.get("cardIndex1"));
                    Integer cardIndex2 = convertToInteger(data.get("cardIndex2"));
                    
                    if (cardIndex1 != null && cardIndex2 != null) {
                        Platform.runLater(() -> {
                            // This handler is now mostly for score/state sync, actual disappearance is via MATCH_RESULT
                            // markCardsAsMatched(cardIndex1, cardIndex2); // No longer needed here
                        });
                    }
                }
            });
            
            client.onMessage("TURN_SWITCH", message -> {
                System.out.println("[GameScreen] Received TURN_SWITCH message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Platform.runLater(() -> {
                        isMyTurn = !isMyTurn;
                        System.out.println("[GameScreen] Turn switched via TCP. Is my turn: " + isMyTurn);
                        resetTurnTimer(); // Reset timer for the new turn
                    });
                }
            });
            
            client.onMessage("CARDS_FLIP_BACK", message -> {
                System.out.println("[GameScreen] Received CARDS_FLIP_BACK message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Integer cardIndex1 = convertToInteger(data.get("cardIndex1"));
                    Integer cardIndex2 = convertToInteger(data.get("cardIndex2"));
                    
                    Platform.runLater(() -> {
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000); // Same delay as sender for flip back
                                Platform.runLater(() -> {
                                    if (cardIndex1 != null && cardIndex1 >= 0 && cardIndex1 < cards.size()) {
                                        cards.get(cardIndex1).flipToBack();
                                    }
                                    if (cardIndex2 != null && cardIndex2 >= 0 && cardIndex2 < cards.size()) {
                                        cards.get(cardIndex2).flipToBack();
                                    }
                                    System.out.println("[GameScreen] Cards flipped back via TCP");
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });
                }
            });
            
            client.onMessage("MATCH_RESULT", message -> {
                System.out.println("[GameScreen] Received MATCH_RESULT message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Boolean isMatch = (Boolean) data.get("isMatch");
                    Integer cardIndex1 = convertToInteger(data.get("cardIndex1"));
                    Integer cardIndex2 = convertToInteger(data.get("cardIndex2"));
                    Boolean shouldSwitchTurn = (Boolean) data.get("shouldSwitchTurn");
                    Integer player1Score = convertToInteger(data.get("player1Score"));
                    Integer player2Score = convertToInteger(data.get("player2Score"));
                    
                    System.out.println("[GameScreen] Processing MATCH_RESULT - isMatch: " + isMatch +
                                      ", cardIndex1: " + cardIndex1 + ", cardIndex2: " + cardIndex2 +
                                      ", shouldSwitchTurn: " + shouldSwitchTurn);
                    
                    Platform.runLater(() -> {
                        if (isMatch != null && isMatch) {
                            // Cards match - make them disappear after 1s delay
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000); // wait 1s to let second card finish flip and be visible
                                    Platform.runLater(() -> {
                                        if (cardIndex1 != null && cardIndex1 >= 0 && cardIndex1 < cards.size()) {
                                            cards.get(cardIndex1).setVisible(false);
                                        }
                                        if (cardIndex2 != null && cardIndex2 >= 0 && cardIndex2 < cards.size()) {
                                            cards.get(cardIndex2).setVisible(false);
                                        }
                                        // Update scores
                                        if (player1Score != null) this.player1Score = player1Score;
                                        if (player2Score != null) this.player2Score = player2Score;
                                        System.out.println("[GameScreen] Cards matched! Disappeared after delay. Scores - P1: " + this.player1Score + ", P2: " + this.player2Score);
                                        updateScoreLabels();
                                        resetFlippedCards();
                                        isResolving = false; // Unlock input
                                        resetTurnTimer(); // Reset timer for current player's next turn
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                            return; // Defer rest until disappearance completes
                        } else {
                            // Cards don't match - flip them back after 1s delay
                            System.out.println("[GameScreen] Cards don't match - starting flip back process");
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000); // Show cards briefly (1s)
                                    Platform.runLater(() -> {
                                        System.out.println("[GameScreen] Flipping back cards - cardIndex1: " + cardIndex1 + ", cardIndex2: " + cardIndex2);
                                        if (cardIndex1 != null && cardIndex1 >= 0 && cardIndex1 < cards.size()) {
                                            cards.get(cardIndex1).flipToBack();
                                            System.out.println("[GameScreen] Flipped back card " + cardIndex1);
                                        }
                                        if (cardIndex2 != null && cardIndex2 >= 0 && cardIndex2 < cards.size()) {
                                            cards.get(cardIndex2).flipToBack();
                                            System.out.println("[GameScreen] Flipped back card " + cardIndex2);
                                        }
                                        
                                        // Reset state AFTER flip-back completes
                                        resetFlippedCards();
                                        
                                        // Switch turn if needed
                                        if (shouldSwitchTurn != null && shouldSwitchTurn) {
                                            System.out.println("[GameScreen] Switching turn - was my turn: " + isMyTurn);
                                            isMyTurn = !isMyTurn;
                                            System.out.println("[GameScreen] Turn switched via server. Is my turn: " + isMyTurn);
                                            resetTurnTimer(); // Reset timer for the new turn
                                        } else {
                                            System.out.println("[GameScreen] No turn switch needed - shouldSwitchTurn: " + shouldSwitchTurn);
                                        }
                                        
                                        isResolving = false; // Unlock input
                                        System.out.println("[GameScreen] Cards don't match - flipped back and state reset");
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                            return; // Defer rest until flip-back completes
                        }
                    });
                }
            });
            
            System.out.println("[GameScreen] TCP handlers setup completed");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to setup TCP handlers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendCardFlippedMessage(int cardIndex, int row, int col) {
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("cardIndex", cardIndex);
            data.put("row", row);
            data.put("col", col);
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("CARD_FLIPPED", data, null, null);
            client.sendMessage(message);
            System.out.println("[GameScreen] Sent card flipped message for position: " + row + "," + col);
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send card flipped message: " + e.getMessage());
        }
    }
    
    private void sendCardsForMatchCheck() {
        if (firstFlippedCard == null || secondFlippedCard == null) {
            System.err.println("[GameScreen] Cannot check match - missing cards");
            return;
        }
        
        int cardIndex1 = cards.indexOf(firstFlippedCard);
        int cardIndex2 = cards.indexOf(secondFlippedCard);
        
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("roomId", gameSettings.getRoomId());
            data.put("cardIndex1", cardIndex1);
            data.put("cardIndex2", cardIndex2);
            data.put("isHost", gameSettings.isHost());
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("CARDS_FOR_MATCH_CHECK", data, null, null);
            client.sendMessage(message);
            System.out.println("[GameScreen] Sent cards for match check - indices: " + cardIndex1 + ", " + cardIndex2);
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send cards for match check: " + e.getMessage());
            isResolving = false;
        }
    }
    
    private void flipCardAtPosition(int cardIndex, int row, int col) {
        if (cardIndex >= 0 && cardIndex < cards.size()) {
            MemoryCard card = cards.get(cardIndex);
            if (!card.isFlipped() && !card.isMatched()) {
                card.flipToFront();
                System.out.println("[GameScreen] Flipped card via TCP at position: " + row + "," + col);
            }
        }
    }
    
    private void resetFlippedCards() {
        firstFlippedCard = null;
        secondFlippedCard = null;
        flippedCardsCount = 0;
        System.out.println("[GameScreen] Reset flipped cards state");
    }
    
    private void updateScoreLabels() {
        if (gameSettings != null) {
            if (gameSettings.isHost()) {
                // Host: myScore = player1Score, opponentScore = player2Score
                if (myScoreLabel != null) {
                    myScoreLabel.setText(String.valueOf(player1Score));
                }
                if (opponentScoreLabel != null) {
                    opponentScoreLabel.setText(String.valueOf(player2Score));
                }
            } else {
                // Guest: myScore = player2Score, opponentScore = player1Score
                if (myScoreLabel != null) {
                    myScoreLabel.setText(String.valueOf(player2Score));
                }
                if (opponentScoreLabel != null) {
                    opponentScoreLabel.setText(String.valueOf(player1Score));
                }
            }
        } else {
            // Fallback
            if (myScoreLabel != null) {
                myScoreLabel.setText(String.valueOf(player1Score));
            }
            if (opponentScoreLabel != null) {
                opponentScoreLabel.setText(String.valueOf(player2Score));
            }
        }
    }
    
    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        currentTurnTime = initialTurnTime;
        updateTimerDisplay();
        
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentTurnTime--;
            updateTimerDisplay();
            
            if (currentTurnTime <= 0) {
                handleTimerEnd();
            }
        }));
        turnTimer.setCycleCount(Timeline.INDEFINITE);
        turnTimer.play();
    }
    
    private void resetTurnTimer() {
        startTurnTimer();
    }
    
    private void updateTimerDisplay() {
        if (timerLabel != null) {
            timerLabel.setText(currentTurnTime + "s");
        }
    }
    
    private void handleTimerEnd() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        if (isMyTurn) {
            // Time's up for current player - switch turn
            isMyTurn = false;
            System.out.println("[GameScreen] Timer ended - switching turn");
            
            // Send turn switch message to opponent
            try {
                TCPClient client = TCPClient.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("switchTurn", true);
                
                TCPClient.TCPMessage message = new TCPClient.TCPMessage("TURN_SWITCH", data, null, null);
                client.sendMessage(message);
            } catch (Exception e) {
                System.err.println("[GameScreen] Failed to send turn switch: " + e.getMessage());
            }
        }
        
        resetTurnTimer();
    }
    
    private int parseTimeSetting(String timeSetting) {
        try {
            return Integer.parseInt(timeSetting.replace("s", ""));
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to parse time setting: " + timeSetting);
            return 30; // Default
        }
    }
    
    private Integer convertToInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
