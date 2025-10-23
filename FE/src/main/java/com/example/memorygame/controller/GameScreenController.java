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
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
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
    
    // UI Components
    @FXML private StackPane gameContainer;
    @FXML private GridPane cardGrid;
    @FXML private ImageView backgroundImage;
    @FXML private Button backButton;
    @FXML private ImageView myAvatar;
    @FXML private ImageView opponentAvatar;
    @FXML private Label myScoreLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private TextField myChatField;
    @FXML private TextField opponentChatField;
    @FXML private Label timerLabel;
    
    private GameScreen screen;
    
    public GameScreenController() {
        // Constructor without parameters - controller will be injected by FXML
        // Get settings from static variable
        this.gameSettings = staticGameSettings;
        
        // Initialize turn based on host/guest status
        if (gameSettings != null) {
            isMyTurn = gameSettings.isHost(); // Host starts first
        }
        
        setupTCPHandlers();
    }
    
    public GameScreenController(GameSettings settings) {
        this.gameSettings = settings;
        // Constructor with settings - for programmatic creation
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
        restartTurnTimer();
    }
    
    private void setupGame() {
        if (gameSettings == null) {
            System.err.println("[GameScreen] No game settings provided!");
            return;
        }
        
        // Setup theme background
        setupThemeBackground();
        
        // Setup card grid
        setupCardGrid();
        
        System.out.println("[GameScreen] Game setup completed");
    }

    private void setupOverlayUI() {
        if (backButton != null) {
            backButton.setOnAction(e -> handleBack());
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
        } catch (Exception ignored) {}
        
        updateScoreLabels();
        updateTimerLabel();
    }

    private void handleBack() {
        try {
            stopTurnTimer();
            Stage stage = (Stage) gameContainer.getScene().getWindow();
            // Use controller-driven loader so RoomScreen initializes helpers/APIs correctly
            RoomScreenController controller = new RoomScreenController();
            Scene scene = new Scene(controller.getScreen().getRoot());
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to go back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int parseTurnSeconds() {
        if (gameSettings == null || gameSettings.getTime() == null) return 10; // default
        String t = gameSettings.getTime().trim().toLowerCase();
        try {
            if (t.endsWith("s")) t = t.substring(0, t.length()-1);
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private Timeline turnTimer;
    private int turnDurationSeconds;
    private int turnSecondsRemaining;

    private void restartTurnTimer() {
        stopTurnTimer();
        turnDurationSeconds = parseTurnSeconds();
        turnSecondsRemaining = turnDurationSeconds;
        updateTimerLabel();
        startTurnTimer();
    }

    private void startTurnTimer() {
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            turnSecondsRemaining = Math.max(0, turnSecondsRemaining - 1);
            updateTimerLabel();
            if (turnSecondsRemaining <= 0) {
                stopTurnTimer();
                onTurnTimeout();
            }
        }));
        turnTimer.setCycleCount(Timeline.INDEFINITE);
        turnTimer.play();
    }

    private void stopTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) {
            Platform.runLater(() -> timerLabel.setText(turnSecondsRemaining + "s"));
        }
    }

    private void onTurnTimeout() {
        System.out.println("[GameScreen] Turn timeout. Current isMyTurn=" + isMyTurn);
        if (isMyTurn) {
            // Flip back any currently flipped, then switch turn
            Platform.runLater(() -> {
                if (secondFlippedCard != null) {
                    secondFlippedCard.flipToBack();
                }
                if (firstFlippedCard != null) {
                    firstFlippedCard.flipToBack();
                }
                resetFlippedCards();
                isResolving = false;
                // Notify other player and switch locally
                sendTurnSwitchMessage();
                isMyTurn = false;
                restartTurnTimer();
            });
        } else {
            // Not my turn: do nothing on local timeout; timer will be restarted when TURN_SWITCH arrives
        }
    }

    private void sendTurnSwitchMessage() {
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("isHost", gameSettings.isHost());
            TCPClient.TCPMessage msg = new TCPClient.TCPMessage("TURN_SWITCH", data, null, null);
            client.sendMessage(msg);
            System.out.println("[GameScreen] Sent TURN_SWITCH due to timeout");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send TURN_SWITCH: " + e.getMessage());
        }
    }

    private void updateScoreLabels() {
        if (myScoreLabel == null || opponentScoreLabel == null) return;
        if (gameSettings != null && gameSettings.isHost()) {
            myScoreLabel.setText(String.valueOf(player1Score));
            opponentScoreLabel.setText(String.valueOf(player2Score));
        } else {
            myScoreLabel.setText(String.valueOf(player2Score));
            opponentScoreLabel.setText(String.valueOf(player1Score));
        }
    }
    
    private void setupThemeBackground() {
        if (gameSettings.getTheme() == null) {
            System.err.println("[GameScreen] No theme provided!");
            return;
        }
        
        ThemeDTO theme = gameSettings.getTheme();
        System.out.println("[GameScreen] Setting up theme: " + theme.name);
        
        // Load background image
        loadBackgroundImage(theme);
    }
    
    private void loadBackgroundImage(ThemeDTO theme) {
        String imagePath = theme.assetPath + "/background.jpg";
        
        try {
            // Load image from server URL
            String serverUrl = "http://localhost:8080" + imagePath;
            javafx.scene.image.Image image = new javafx.scene.image.Image(serverUrl);
            backgroundImage.setImage(image);
            backgroundImage.setPreserveRatio(false); // Disable ratio preservation
            backgroundImage.setVisible(true);
            System.out.println("[GameScreen] Background image loaded: " + serverUrl);
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load image: " + e.getMessage());
            // Try fallback to local resource
            loadFallbackBackground();
        }
    }
    
    private void loadFallbackBackground() {
        try {
            // Load default background from local resources
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/com/example/memorygame/assets/images/default_bg.jpg")
            );
            backgroundImage.setImage(image);
            backgroundImage.setPreserveRatio(false); // Disable ratio preservation
            backgroundImage.setVisible(true);
            System.out.println("[GameScreen] Fallback background loaded");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load fallback background: " + e.getMessage());
        }
    }
    
    private void setupCardGrid() {
        if (gameSettings.getSize() == null) {
            System.err.println("[GameScreen] No size provided!");
            return;
        }
        
        String size = gameSettings.getSize();
        System.out.println("[GameScreen] Setting up card grid with size: " + size);
        
        // Parse size (e.g., "5x6" -> rows=5, cols=6)
        String[] dimensions = size.split("x");
        if (dimensions.length != 2) {
            System.err.println("[GameScreen] Invalid size format: " + size);
            return;
        }
        
        try {
            int rows = Integer.parseInt(dimensions[0]);
            int cols = Integer.parseInt(dimensions[1]);
            
            // Setup grid dimensions
            cardGrid.getRowConstraints().clear();
            cardGrid.getColumnConstraints().clear();
            
            for (int i = 0; i < rows; i++) {
                javafx.scene.layout.RowConstraints row = new javafx.scene.layout.RowConstraints();
                row.setPercentHeight(100.0 / rows);
                cardGrid.getRowConstraints().add(row);
            }
            
            for (int i = 0; i < cols; i++) {
                javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
                col.setPercentWidth(100.0 / cols);
                cardGrid.getColumnConstraints().add(col);
            }
            
            // Create cards
            createCards(rows, cols);
            
            System.out.println("[GameScreen] Card grid setup: " + rows + "x" + cols);
            
        } catch (NumberFormatException e) {
            System.err.println("[GameScreen] Invalid size numbers: " + size);
        }
    }
    
    private void createCards(int rows, int cols) {
        System.out.println("[GameScreen] Creating " + (rows * cols) + " cards");
        
        // Load cards from server
        loadCardsFromServer(rows, cols);
    }
    
    private void loadCardsFromServer(int rows, int cols) {
        try {
            String themeName = gameSettings.getTheme().name;
            String size = gameSettings.getSize();
            
            System.out.println("[GameScreen] Loading cards for theme: " + themeName + ", size: " + size);
            
            // Get cards from server
            List<CardDTO> serverCards = CardApi.getCardsForGame(themeName, size, gameSettings.getRoomId());
            
            if (serverCards.isEmpty()) {
                System.err.println("[GameScreen] No cards received from server, creating mock cards");
                createMockCards(rows, cols);
                return;
            }
            
            System.out.println("[GameScreen] Received " + serverCards.size() + " cards from server");
            
            // Store card data
            this.cardData = new ArrayList<>(serverCards);
            
            // Log first few cards for debugging
            for (int i = 0; i < Math.min(5, serverCards.size()); i++) {
                System.out.println("[GameScreen] Received Card " + i + ": " + serverCards.get(i).getImagePath());
            }
            
            // Create memory cards
            createMemoryCards(rows, cols);
            
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load cards from server: " + e.getMessage());
            e.printStackTrace();
            createMockCards(rows, cols);
        }
    }
    
    private void createMemoryCards(int rows, int cols) {
        // Clear existing cards
        cardGrid.getChildren().clear();
        cards.clear();
        
        // Get card back path
        String cardBackPath = "http://localhost:8080" + gameSettings.getTheme().assetPath + "/card_back.png";
        
        // Create memory cards
        for (int i = 0; i < cardData.size(); i++) {
            CardDTO cardDTO = cardData.get(i);
            MemoryCard memoryCard = new MemoryCard(cardDTO, cardBackPath);
            
            // Add TCP synchronization
            final int cardIndex = i;
            final int finalRow = i / cols;
            final int finalCol = i % cols;
            
            memoryCard.setOnAction(e -> {
                System.out.println("[GameScreen] Card clicked at position: " + finalRow + "," + finalCol);
                
                // Check if it's my turn
                if (!isMyTurn) {
                    System.out.println("[GameScreen] Not my turn, ignoring click");
                    return;
                }
                // Prevent interaction while resolving previous turn
                if (isResolving) {
                    System.out.println("[GameScreen] Resolving previous result, ignoring click");
                    return;
                }
                
                // Check if card can be flipped
                if (memoryCard.isFlipped() || memoryCard.isMatched()) {
                    System.out.println("[GameScreen] Card already flipped or matched, ignoring click");
                    return;
                }
                
                // Check if we already have 2 cards flipped
                if (flippedCardsCount >= 2) {
                    System.out.println("[GameScreen] Already have 2 cards flipped, ignoring click");
                    return;
                }
                
                // Flip the card locally first
                memoryCard.flipToFront();
                flippedCardsCount++;
                System.out.println("[GameScreen] Flipped card locally at position: " + finalRow + "," + finalCol + " (flipped: " + flippedCardsCount + ")");
                
                // Store flipped card
                if (firstFlippedCard == null) {
                    firstFlippedCard = memoryCard;
                    System.out.println("[GameScreen] Set firstFlippedCard at position: " + finalRow + "," + finalCol);
                } else {
                    secondFlippedCard = memoryCard;
                    System.out.println("[GameScreen] Set secondFlippedCard at position: " + finalRow + "," + finalCol + " - sending to server for match check");
                    isResolving = true; // lock input until result handled
                    // Send cards to server for match check after 2 cards are flipped
                    sendCardsForMatchCheck();
                }
                
                // Send TCP message to synchronize with other player
                sendCardFlippedMessage(cardIndex, finalRow, finalCol);
            });
            
            cards.add(memoryCard);
            
            // Add to grid
            cardGrid.add(memoryCard, finalCol, finalRow);
        }
        
        System.out.println("[GameScreen] Created " + cards.size() + " memory cards");
    }
    
    private void createMockCards(int rows, int cols) {
        // Clear existing cards
        cardGrid.getChildren().clear();
        cards.clear();
        
        int totalCards = rows * cols;
        int pairs = totalCards / 2;
        
        System.out.println("[GameScreen] Creating " + totalCards + " mock cards (" + pairs + " pairs)");
        
        // Create mock card data
        this.cardData = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            // Create pair of cards
            CardDTO card1 = new CardDTO((long) (i * 2), "/static/themes/Chirstmas/card_" + String.format("%02d", i + 1) + ".png");
            CardDTO card2 = new CardDTO((long) (i * 2 + 1), "/static/themes/Chirstmas/card_" + String.format("%02d", i + 1) + ".png");
            
            cardData.add(card1);
            cardData.add(card2);
        }
        
        // Shuffle cards
        Collections.shuffle(cardData);
        
        // Create memory cards
        createMemoryCards(rows, cols);
    }
    
    /**
     * Setup resize listener to make background responsive to window size changes
     */
    private void setupResizeListener() {
        if (gameContainer != null) {
            gameContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
                updateBackgroundSize();
            });
            
            gameContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                updateBackgroundSize();
            });
        }
    }
    
    /**
     * Update background image size to fit the container
     */
    private void updateBackgroundSize() {
        if (backgroundImage != null && gameContainer != null) {
            double containerWidth = gameContainer.getWidth();
            double containerHeight = gameContainer.getHeight();
            
            if (containerWidth > 0 && containerHeight > 0) {
                backgroundImage.setFitWidth(containerWidth);
                backgroundImage.setFitHeight(containerHeight);
                backgroundImage.setPreserveRatio(false); // Disable ratio preservation to fill entire container
                System.out.println("[GameScreen] Background resized to: " + containerWidth + "x" + containerHeight);
            }
        }
    }
    
    
    /**
     * Reset flipped cards state
     */
    private void resetFlippedCards() {
        firstFlippedCard = null;
        secondFlippedCard = null;
        flippedCardsCount = 0;
    }
    
    
    /**
     * Send cards for match check to server
     */
    private void sendCardsForMatchCheck() {
        System.out.println("[GameScreen] sendCardsForMatchCheck() called");
        
        if (firstFlippedCard == null || secondFlippedCard == null) {
            System.err.println("[GameScreen] ERROR: Cannot send match check - firstFlippedCard: " + firstFlippedCard + ", secondFlippedCard: " + secondFlippedCard);
            return;
        }
        
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            
            // Find card indices
            int cardIndex1 = firstFlippedCard != null ? cards.indexOf(firstFlippedCard) : -1;
            int cardIndex2 = secondFlippedCard != null ? cards.indexOf(secondFlippedCard) : -1;
            
            System.out.println("[GameScreen] Card indices - cardIndex1: " + cardIndex1 + ", cardIndex2: " + cardIndex2);
            
            data.put("cardIndex1", cardIndex1);
            data.put("cardIndex2", cardIndex2);
            data.put("roomId", gameSettings.getRoomId());
            data.put("isHost", gameSettings.isHost());
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("CARDS_FOR_MATCH_CHECK", data, null, null);
            client.sendMessage(message);
            System.out.println("[GameScreen] Sent CARDS_FOR_MATCH_CHECK message: " + data);
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send cards for match check: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    /**
     * Convert Object to Integer, handling both Integer and Double types
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * Setup TCP message handlers for game synchronization
     */
    private void setupTCPHandlers() {
        try {
            TCPClient client = TCPClient.getInstance();
            
            // Handle card flipped messages from other players
            client.onMessage("CARD_FLIPPED", message -> {
                System.out.println("[GameScreen] Received CARD_FLIPPED message: " + message.getData());
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Integer cardIndex = convertToInteger(data.get("cardIndex"));
                    Integer row = convertToInteger(data.get("row"));
                    Integer col = convertToInteger(data.get("col"));
                    
                    if (cardIndex != null && row != null && col != null) {
                        javafx.application.Platform.runLater(() -> {
                            flipCardAtPosition(cardIndex, row, col);
                        });
                    }
                }
            });
            
                // Handle card matched messages from other players
                client.onMessage("CARD_MATCHED", message -> {
                    System.out.println("[GameScreen] Received CARD_MATCHED message: " + message.getData());
                    Map<String, Object> data = message.getData();
                    if (data != null) {
                        Integer cardIndex1 = convertToInteger(data.get("cardIndex1"));
                        Integer cardIndex2 = convertToInteger(data.get("cardIndex2"));
                        Boolean isHost = (Boolean) data.get("isHost");
                        Integer score = convertToInteger(data.get("score"));
                        
                        if (cardIndex1 != null && cardIndex2 != null) {
                            javafx.application.Platform.runLater(() -> {
                                markCardsAsMatched(cardIndex1, cardIndex2);
                                
                                // Update score
                                if (isHost != null && score != null) {
                                    if (isHost) {
                                        player1Score = score;
                                    } else {
                                        player2Score = score;
                                    }
                                    System.out.println("[GameScreen] Updated scores - Player1: " + player1Score + ", Player2: " + player2Score);
                                }
                            });
                        }
                    }
                });
                
                // Handle turn switch messages from other players
                client.onMessage("TURN_SWITCH", message -> {
                    System.out.println("[GameScreen] Received TURN_SWITCH message: " + message.getData());
                    Map<String, Object> data = message.getData();
                    if (data != null) {
                        javafx.application.Platform.runLater(() -> {
                            // Switch turn
                            isMyTurn = !isMyTurn;
                            System.out.println("[GameScreen] Turn switched via TCP. Is my turn: " + isMyTurn);
                            
                            // Don't reset flipped cards here - wait for CARDS_FLIP_BACK message
                            restartTurnTimer();
                        });
                    }
                });
                
                // Handle cards flip back messages from other players
                client.onMessage("CARDS_FLIP_BACK", message -> {
                    System.out.println("[GameScreen] Received CARDS_FLIP_BACK message: " + message.getData());
                    Map<String, Object> data = message.getData();
                    if (data != null) {
                        Integer cardIndex1 = convertToInteger(data.get("cardIndex1"));
                        Integer cardIndex2 = convertToInteger(data.get("cardIndex2"));
                        
                        javafx.application.Platform.runLater(() -> {
                            // Flip back the cards after a delay (to match the sender's timing)
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000); // Same delay as sender
                                    
                                    javafx.application.Platform.runLater(() -> {
                                        if (cardIndex1 != null && cardIndex1 >= 0 && cardIndex1 < cards.size()) {
                                            cards.get(cardIndex1).flipToBack();
                                        }
                                        if (cardIndex2 != null && cardIndex2 >= 0 && cardIndex2 < cards.size()) {
                                            cards.get(cardIndex2).flipToBack();
                                        }
                                        
                                        // Reset flipped cards
                                        resetFlippedCards();
                                        
                                        System.out.println("[GameScreen] Cards flipped back via TCP");
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        });
                    }
                });
                
                // Handle match result from server
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
                        
                        javafx.application.Platform.runLater(() -> {
                            if (isMatch != null && isMatch) {
                                // Cards match - show both for 1s, then make them disappear; keep turn
                                isResolving = true;
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(1000); // wait 1s to let second card finish flip and be visible
                                        javafx.application.Platform.runLater(() -> {
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
                                            isResolving = false;
                                            restartTurnTimer();
                                        });
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }).start();
                                return;
                            } else {
                                // Cards don't match - flip them back after delay
                                System.out.println("[GameScreen] Cards don't match - starting flip back process");
                                isResolving = true;
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(1000); // Show cards briefly (1s)
                                        
                                        javafx.application.Platform.runLater(() -> {
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
                                            } else {
                                                System.out.println("[GameScreen] No turn switch needed - shouldSwitchTurn: " + shouldSwitchTurn);
                                            }
                                            
                                            isResolving = false;
                                            System.out.println("[GameScreen] Cards don't match - flipped back and state reset");
                                            updateScoreLabels();
                                            restartTurnTimer();
                                        });
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }).start();
                                return; // Defer rest until flip-back completes
                            }
                            // No immediate reset here; both branches handle reset and resolving state
                        });
                    }
                });
            
            System.out.println("[GameScreen] TCP handlers setup completed");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to setup TCP handlers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Flip card at specific position (called when receiving TCP message)
     */
    private void flipCardAtPosition(int cardIndex, int row, int col) {
        // Don't flip if we're currently sending a message (to prevent double-flip)
        if (isSendingMessage) {
            System.out.println("[GameScreen] Ignoring TCP flip message - currently sending message");
            return;
        }
        
        if (cardIndex >= 0 && cardIndex < cards.size()) {
            MemoryCard card = cards.get(cardIndex);
            if (!card.isFlipped() && !card.isMatched()) {
                card.flipToFront();
                flippedCardsCount++;
                System.out.println("[GameScreen] Flipped card at position " + row + "," + col + " from TCP message (flipped: " + flippedCardsCount + ")");
                
                // Store flipped card for matching logic
                if (firstFlippedCard == null) {
                    firstFlippedCard = card;
                } else {
                    secondFlippedCard = card;
                }
            }
        }
    }
    
    /**
     * Mark cards as matched (called when receiving TCP message)
     */
    private void markCardsAsMatched(int cardIndex1, int cardIndex2) {
        if (cardIndex1 >= 0 && cardIndex1 < cards.size() && 
            cardIndex2 >= 0 && cardIndex2 < cards.size()) {
            cards.get(cardIndex1).markAsMatched();
            cards.get(cardIndex2).markAsMatched();
            System.out.println("[GameScreen] Marked cards " + cardIndex1 + " and " + cardIndex2 + " as matched from TCP message");
        }
    }
    
    /**
     * Send card flipped message via TCP to synchronize with other player
     */
    private void sendCardFlippedMessage(int cardIndex, int row, int col) {
        try {
            isSendingMessage = true; // Set flag to prevent double-flip
            
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("cardIndex", cardIndex);
            data.put("row", row);
            data.put("col", col);
            data.put("imagePath", cardData.get(cardIndex).getImagePath());
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("CARD_FLIPPED", data, null, null);
            client.sendMessage(message);
            System.out.println("[GameScreen] Sent CARD_FLIPPED message: " + data);
            
            // Reset flag after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(100); // Small delay to ensure message is sent
                    isSendingMessage = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send card flipped message: " + e.getMessage());
            e.printStackTrace();
            isSendingMessage = false; // Reset flag on error
        }
    }
    
    // Getters and setters
    public GameSettings getGameSettings() {
        return gameSettings;
    }
}
