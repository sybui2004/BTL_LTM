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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.FXMLLoader;
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
    private Thread matchCheckTimeoutThread = null; // Thread to handle timeout if server doesn't respond
    
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
    @FXML private Label turnIndicatorLabel;
    
    private GameScreen screen;
    private volatile boolean gameEndReceived = false;
    
    public GameScreenController() {
        this.gameSettings = staticGameSettings;
        
        if (gameSettings != null) {
            // Determine who goes first based on coin flip result
            boolean hostFirstTurn = gameSettings.isHostFirstTurn();
            if (gameSettings.isHost()) {
                // I am host - I go first if hostFirstTurn is true
                isMyTurn = hostFirstTurn;
            } else {
                // I am guest - I go first if hostFirstTurn is false
                isMyTurn = !hostFirstTurn;
            }
            initialTurnTime = parseTimeSetting(gameSettings.getTime());
            System.out.println("[GameScreen] Turn initialization - hostFirstTurn: " + hostFirstTurn + 
                             ", isHost: " + gameSettings.isHost() + ", isMyTurn: " + isMyTurn);
        } else {
            isMyTurn = true; // Default
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
        updateTurnIndicator();
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
        // Show confirmation dialog
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thoát");
        alert.setHeaderText("Bạn có chắc chắn muốn thoát khỏi trò chơi?");
        alert.setContentText("Nếu thoát, bạn sẽ mất tiến trình hiện tại.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    // Send a surrender message to backend
                    // Backend will handle game exit and send GAME_END to opponent
                    // We do NOT disconnect here - user is still online, just leaving the game
                    try {
                        TCPClient client = TCPClient.getInstance();
                        Map<String, Object> data = new HashMap<>();
                        data.put("roomId", gameSettings != null ? gameSettings.getRoomId() : null);
                        TCPClient.TCPMessage surrenderMessage = new TCPClient.TCPMessage("PLAYER_SURRENDER", data, null, null);
                        client.sendMessage(surrenderMessage);
                        System.out.println("[GameScreen] Sent PLAYER_SURRENDER message - staying connected");
                        // Additionally call REST exitRoom to persist room state immediately (defensive)
                        new Thread(() -> {
                            try {
                                com.example.memorygame.model.user.UserSummary me = com.example.memorygame.utils.UserApi.getCurrentUser();
                                if (me != null && gameSettings != null && gameSettings.getRoomId() != null) {
                                    boolean ok = com.example.memorygame.utils.RoomApi.exitRoom(gameSettings.getRoomId(), me.id);
                                    System.out.println("[GameScreen] Called REST exitRoom => " + ok);
                                }
                            } catch (Exception ignore) {}
                        }).start();
                    } catch (Exception e) {
                        System.err.println("[GameScreen] Failed to send surrender message: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    Stage stage = (Stage) backButton.getScene().getWindow();
                    
                    // Stop the timer if it's running
                    if (turnTimer != null) {
                        turnTimer.stop();
                    }
                    
                    // Navigate back to MainScreen
                    MainScreenController mainController = new MainScreenController();
                    Scene mainScene = new Scene(mainController.getScreen().getRoot());
                    mainScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
                    
                    stage.setScene(mainScene);
                    stage.setTitle("Memory Matching Game");
                    stage.show();
                    
                    System.out.println("[GameScreen] Navigated back to main screen");
                    
                } catch (Exception e) {
                    System.err.println("[GameScreen] Failed to navigate back: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
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
                
                // Verify card count matches expected grid size
                int expectedCards = rows * cols;
                if (fetchedCards.size() != expectedCards) {
                    System.out.println("[GameScreen] Warning: Card count mismatch. Expected: " + expectedCards + 
                                     ", Got: " + fetchedCards.size() + ". Server may have adjusted due to insufficient theme cards.");
                }
                
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
                        updateTurnIndicator();
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
                    
                    // Cancel timeout since we received response
                    if (matchCheckTimeoutThread != null && matchCheckTimeoutThread.isAlive()) {
                        matchCheckTimeoutThread.interrupt();
                    }
                    
                    // Save card references BEFORE Platform.runLater to avoid race conditions
                    final MemoryCard savedCard1 = (cardIndex1 != null && cardIndex1 >= 0 && cardIndex1 < cards.size()) 
                        ? cards.get(cardIndex1) 
                        : firstFlippedCard;
                    final MemoryCard savedCard2 = (cardIndex2 != null && cardIndex2 >= 0 && cardIndex2 < cards.size()) 
                        ? cards.get(cardIndex2) 
                        : secondFlippedCard;
                    
                    System.out.println("[GameScreen] Saved card references - card1: " + (savedCard1 != null ? savedCard1.getCardData().getId() : "null") +
                                      ", card2: " + (savedCard2 != null ? savedCard2.getCardData().getId() : "null"));
                    
                    Platform.runLater(() -> {
                        // Default to false if isMatch is null
                        boolean cardsMatch = (isMatch != null && isMatch);
                        
                        if (cardsMatch) {
                            // Cards match - make them disappear after 1s delay
                            System.out.println("[GameScreen] Cards match - will disappear after delay");
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000); // wait 1s to let second card finish flip and be visible
                                    Platform.runLater(() -> {
                                        if (savedCard1 != null) {
                                            savedCard1.setVisible(false);
                                        }
                                        if (savedCard2 != null) {
                                            savedCard2.setVisible(false);
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
                            // Cards don't match (or isMatch is null) - flip them back after 1s delay
                            System.out.println("[GameScreen] Cards don't match (isMatch=" + isMatch + ") - starting flip back process");
                            System.out.println("[GameScreen] Card1 flipped state: " + (savedCard1 != null ? savedCard1.isFlipped() : "null") +
                                              ", Card2 flipped state: " + (savedCard2 != null ? savedCard2.isFlipped() : "null"));
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000); // Show cards briefly (1s)
                                    Platform.runLater(() -> {
                                        System.out.println("[GameScreen] Flipping back cards after 1s delay");
                                        
                                        // Flip back both cards if they are flipped
                                        if (savedCard1 != null && savedCard1.isFlipped()) {
                                            System.out.println("[GameScreen] Flipping back card 1");
                                            savedCard1.flipToBack();
                                        } else if (savedCard1 != null) {
                                            System.out.println("[GameScreen] Card 1 is not flipped, skipping");
                                        } else {
                                            System.out.println("[GameScreen] Card 1 is null, cannot flip back");
                                        }
                                        
                                        if (savedCard2 != null && savedCard2.isFlipped()) {
                                            System.out.println("[GameScreen] Flipping back card 2");
                                            savedCard2.flipToBack();
                                        } else if (savedCard2 != null) {
                                            System.out.println("[GameScreen] Card 2 is not flipped, skipping");
                                        } else {
                                            System.out.println("[GameScreen] Card 2 is null, cannot flip back");
                                        }
                                        
                                        // Reset state AFTER flip-back completes
                                        resetFlippedCards();
                                        
                                        // Switch turn if needed
                                        if (shouldSwitchTurn != null && shouldSwitchTurn) {
                                            System.out.println("[GameScreen] Switching turn - was my turn: " + isMyTurn);
                                            isMyTurn = !isMyTurn;
                                            System.out.println("[GameScreen] Turn switched via server. Is my turn: " + isMyTurn);
                                            updateTurnIndicator();
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
            
            client.onMessage("GAME_END", message -> {
                System.out.println("[GameScreen] ✓✓✓ Received GAME_END message: " + message.getData());
                System.out.println("[GameScreen] Message type: " + message.getType());
                
                // Prevent duplicate processing
                if (gameEndReceived) {
                    System.out.println("[GameScreen] GAME_END already processed, ignoring duplicate");
                    return;
                }
                
                // Set flag IMMEDIATELY to prevent race with GUEST_LEFT/HOST_PROMOTED
                gameEndReceived = true;
                System.out.println("[GameScreen] Set gameEndReceived=true immediately to prevent race conditions");
                
                Map<String, Object> data = message.getData();
                if (data != null) {
                    System.out.println("[GameScreen] Processing GAME_END data: " + data);
                    // Run handleGameEnd in background thread to avoid blocking JavaFX thread
                    new Thread(() -> {
                        System.out.println("[GameScreen] Calling handleGameEnd in background thread");
                        handleGameEnd(data);
                    }).start();
                } else {
                    System.err.println("[GameScreen] ✗ GAME_END message has null data!");
                }
            });

            // Also listen to fallback signals from Room service
            client.onMessage("GUEST_LEFT", message -> {
                System.out.println("[GameScreen] Received GUEST_LEFT while in game - scheduling fallback");
                if (!gameEndReceived) {
                    scheduleOpponentLeftFallback();
                } else {
                    System.out.println("[GameScreen] GUEST_LEFT received after game end, ignoring");
                }
            });

            client.onMessage("HOST_PROMOTED", message -> {
                System.out.println("[GameScreen] Received HOST_PROMOTED while in game - scheduling fallback");
                if (!gameEndReceived) {
                    scheduleOpponentLeftFallback();
                } else {
                    System.out.println("[GameScreen] HOST_PROMOTED received after game end, ignoring");
                }
            });
            
            System.out.println("[GameScreen] TCP handlers setup completed");
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to setup TCP handlers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleGameEnd(Map<String, Object> data) {
        System.out.println("[GameScreen] handleGameEnd called with data: " + data);
        
        // Delay longer to ensure last match animation completes (1s delay in MATCH_RESULT + buffer)
        try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        
        // Update scores from server BEFORE showing dialog
        Integer player1ScoreFromServer = convertToInteger(data.get("player1Score"));
        Integer player2ScoreFromServer = convertToInteger(data.get("player2Score"));
        
        if (player1ScoreFromServer != null) this.player1Score = player1ScoreFromServer;
        if (player2ScoreFromServer != null) this.player2Score = player2ScoreFromServer;
        
        // Get matchId and call finishMatch API (fallback to settings.matchId if message misses it)
        Object matchIdObj = data.get("matchId");
        Long matchId = null;
        if (matchIdObj != null) {
            try {
                matchId = (matchIdObj instanceof Number)
                    ? ((Number) matchIdObj).longValue()
                    : Long.parseLong(matchIdObj.toString());
            } catch (Exception e) {
                System.err.println("[GameScreen] Failed to parse matchId from message: " + e.getMessage());
            }
        }
        if (matchId == null && gameSettings != null && gameSettings.getMatchId() != null) {
            matchId = gameSettings.getMatchId();
            System.out.println("[GameScreen] Using matchId from GameSettings as fallback: " + matchId);
        }
        
        if (matchId != null && player1ScoreFromServer != null && player2ScoreFromServer != null) {
            Long finalMatchId = matchId;
            new Thread(() -> {
                try {
                    boolean success = com.example.memorygame.utils.MatchApi.finishMatch(
                        finalMatchId,
                        player1ScoreFromServer,
                        player2ScoreFromServer
                    );
                    if (success) {
                        System.out.println("[GameScreen] Successfully finished match " + finalMatchId);
                    } else {
                        System.err.println("[GameScreen] Failed to finish match " + finalMatchId);
                    }
                } catch (Exception e) {
                    System.err.println("[GameScreen] Error calling finishMatch API: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        } else {
            System.out.println("[GameScreen] Missing matchId or scores; cannot call finishMatch. matchId=" + matchId +
                ", p1=" + player1ScoreFromServer + ", p2=" + player2ScoreFromServer);
        }
        
        // Update score labels on UI and hide any remaining visible cards
        Platform.runLater(() -> {
            updateScoreLabels();
            System.out.println("[GameScreen] Updated scores - P1: " + this.player1Score + ", P2: " + this.player2Score);
            
            // Hide any cards that are still visible (in case last match animation)
            for (MemoryCard card : cards) {
                if (card != null && card.isVisible() && !card.isMatched()) {
                    card.setVisible(false);
                    System.out.println("[GameScreen] Hiding remaining visible card");
                }
            }
        });
        
        // Wait a bit more for UI update
        try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        
        // Get game end data
        String winnerUsername = (String) data.get("winnerUsername");
        Object winnerIdObj = data.get("winnerId");
        Boolean isSurrender = (Boolean) data.get("isSurrender");
        Integer winnerRankPoints = convertToInteger(data.get("winnerRankPoints"));
        Integer loserRankPoints = convertToInteger(data.get("loserRankPoints"));
        
        // Determine if I won - prefer comparing winnerId to my id (robust), fallback to username
        boolean iWon = false;
        try {
            com.example.memorygame.model.user.UserSummary currentUser = com.example.memorygame.utils.UserApi.getCurrentUser();
            if (currentUser != null) {
                if (winnerIdObj != null) {
                    Long myIdBoxed = currentUser.id;
                    long myId = (myIdBoxed != null) ? myIdBoxed.longValue() : -1L;
                    long winnerIdParsed;
                    try {
                        winnerIdParsed = (long) Math.round(Double.parseDouble(winnerIdObj.toString()));
                    } catch (NumberFormatException nfe) {
                        winnerIdParsed = -2L;
                    }
                    iWon = (myId != -1L && winnerIdParsed != -2L && winnerIdParsed == myId);
                }
                if (!iWon && winnerUsername != null && currentUser.username != null) {
                    iWon = winnerUsername.equals(currentUser.username);
                }
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to get current user for winner check: " + e.getMessage());
        }
        
        System.out.println("[GameScreen] Game End Debug - currentUsername: " + 
            (com.example.memorygame.utils.UserApi.getCurrentUser() != null ? com.example.memorygame.utils.UserApi.getCurrentUser().username : "null") + 
            ", winnerUsername: " + winnerUsername + ", winnerId: " + winnerIdObj + ", iWon: " + iWon);
        System.out.println("[GameScreen] Scores - P1: " + this.player1Score + ", P2: " + this.player2Score + ", Winner: +" + winnerRankPoints + ", Loser: -" + loserRankPoints);
        
        // Build dialog message
        String message;
        if (isSurrender != null && isSurrender) {
            message = String.format("Đối thủ đã thoát!\n\n");
        } else {
            message = String.format("Tất cả các lá bài đã được mở!\n\n");
        }
        
        // Get display names for winner (use displayName from GameSettings for display)
        String winnerDisplayName = "Unknown";
        if (gameSettings != null) {
            if (iWon) {
                // I won - use my displayName from GameSettings
                winnerDisplayName = gameSettings.isHost() ? gameSettings.getPlayer1Name() : gameSettings.getPlayer2Name();
                System.out.println("[GameScreen] I won - winnerDisplayName: " + winnerDisplayName);
            } else {
                // Opponent won - use opponent's displayName from GameSettings
                winnerDisplayName = gameSettings.isHost() ? gameSettings.getPlayer2Name() : gameSettings.getPlayer1Name();
                System.out.println("[GameScreen] I lost - winnerDisplayName: " + winnerDisplayName);
            }
        } else {
            System.err.println("[GameScreen] gameSettings is null, cannot determine winnerDisplayName");
        }
        
        // Build final message with score info
        String finalMessage;
        if (iWon) {
            finalMessage = message + String.format("Điểm của bạn: %d cặp\nĐiểm đối thủ: %d cặp\n\n" +
                               "Bạn được cộng: +%d điểm rank",
                               gameSettings != null && gameSettings.isHost() ? this.player1Score : this.player2Score,
                               gameSettings != null && gameSettings.isHost() ? this.player2Score : this.player1Score,
                               winnerRankPoints != null ? winnerRankPoints : 0);
        } else {
            finalMessage = message + String.format("Điểm của bạn: %d cặp\nĐiểm đối thủ: %d cặp\n\n" +
                               "Người thắng: %s\n" +
                               "Bạn bị trừ: -%d điểm rank",
                               gameSettings != null && gameSettings.isHost() ? this.player1Score : this.player2Score,
                               gameSettings != null && gameSettings.isHost() ? this.player2Score : this.player1Score,
                               winnerDisplayName,
                               loserRankPoints != null ? loserRankPoints : 0);
        }
        
        String finalWinnerDisplayName = winnerDisplayName;
        boolean finalIWon = iWon;
        
        // Show end game dialog on JavaFX thread
        Platform.runLater(() -> {
            // Stop timer
            if (turnTimer != null) {
                turnTimer.stop();
                System.out.println("[GameScreen] Timer stopped");
            }
            
            // Show custom result popup
            // Calculate my score and opponent score
            int myScore = gameSettings != null && gameSettings.isHost() ? this.player1Score : this.player2Score;
            int opponentScore = gameSettings != null && gameSettings.isHost() ? this.player2Score : this.player1Score;
            
            // Rank points: winnerRankPoints is positive for winner, loserRankPoints is positive for loser (but displayed as negative)
            int myRankPoints, opponentRankPoints;
            if (finalIWon) {
                myRankPoints = winnerRankPoints != null ? winnerRankPoints : 0;
                opponentRankPoints = loserRankPoints != null ? loserRankPoints : 0;
            } else {
                myRankPoints = loserRankPoints != null ? loserRankPoints : 0; // This will be displayed as negative
                opponentRankPoints = winnerRankPoints != null ? winnerRankPoints : 0;
            }
            
            try {
                showGameResultPopup(finalIWon, finalWinnerDisplayName, 
                                   myScore, opponentScore,
                                   myRankPoints, opponentRankPoints);
            } catch (Exception e) {
                System.err.println("[GameScreen] Failed to show custom popup: " + e.getMessage());
                e.printStackTrace();
                // Fallback to Alert
                showGameEndAlert(finalIWon, finalMessage);
            }
        });
    }

    private void showGameResultPopup(boolean iWon, String opponentName, int myScore, int opponentScore, 
                                     int myRankPoints, int opponentRankPoints) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/GameResultPopup.fxml"));
        StackPane popupRoot = loader.load();
        
        // Get current scene root to add CSS
        popupRoot.getStylesheets().add(getClass().getResource("/com/example/memorygame/GameResultPopupStyle.css").toExternalForm());
        
        // Get components
        Label titleLabel = (Label) popupRoot.lookup("#titleLabel");
        ImageView player1Avatar = (ImageView) popupRoot.lookup("#player1Avatar");
        ImageView player2Avatar = (ImageView) popupRoot.lookup("#player2Avatar");
        Label player1Score = (Label) popupRoot.lookup("#player1Score");
        Label player2Score = (Label) popupRoot.lookup("#player2Score");
        Label player1Name = (Label) popupRoot.lookup("#player1Name");
        Label player2Name = (Label) popupRoot.lookup("#player2Name");
        Label player1RankPoints = (Label) popupRoot.lookup("#player1RankPoints");
        Label player2RankPoints = (Label) popupRoot.lookup("#player2RankPoints");
        Button leaveButton = (Button) popupRoot.lookup("#leaveButton");
        Button rematchButton = (Button) popupRoot.lookup("#rematchButton");
        ImageView swordIcon = (ImageView) popupRoot.lookup("#swordIcon");
        
        // Set title and style
        if (iWon) {
            titleLabel.setText("You Win!");
            titleLabel.getStyleClass().removeAll("win", "lose");
            titleLabel.getStyleClass().add("win");
        } else {
            titleLabel.setText("You Lose!");
            titleLabel.getStyleClass().removeAll("win", "lose");
            titleLabel.getStyleClass().add("lose");
        }
        
        // Get user info
        com.example.memorygame.model.user.UserSummary currentUser = com.example.memorygame.utils.UserApi.getCurrentUser();
        String myName = currentUser != null && currentUser.displayName != null ? currentUser.displayName : 
                       (currentUser != null ? currentUser.username : "Player 1");
        String myAvatarUrl = currentUser != null && currentUser.avatarUrl != null ? currentUser.avatarUrl : 
                            "http://localhost:8080/static/avatars/default_avatar.png";
        
        String opponentDisplayName = opponentName != null ? opponentName : "Player 2";
        String opponentAvatarUrl = "http://localhost:8080/static/avatars/default_avatar.png";
        
        // Player 1 is always "me", Player 2 is opponent
        // But layout: if I won, I'm on left; if I lost, opponent is on left
        // Actually, based on image: left player is winner, right is loser
        String winnerName, loserName;
        int winnerScore, loserScore;
        int winnerRankPoints, loserRankPoints;
        String winnerAvatarUrl, loserAvatarUrl;
        
        if (iWon) {
            // I won: I'm left (player1), opponent is right (player2)
            winnerName = myName;
            loserName = opponentDisplayName;
            winnerScore = myScore;
            loserScore = opponentScore;
            winnerRankPoints = myRankPoints;
            loserRankPoints = opponentRankPoints;
            winnerAvatarUrl = myAvatarUrl;
            loserAvatarUrl = opponentAvatarUrl;
        } else {
            // I lost: opponent is left (player1), I'm right (player2)
            winnerName = opponentDisplayName;
            loserName = myName;
            winnerScore = opponentScore;
            loserScore = myScore;
            winnerRankPoints = opponentRankPoints; // This should be positive for winner
            loserRankPoints = myRankPoints; // This should be negative for loser
            winnerAvatarUrl = opponentAvatarUrl;
            loserAvatarUrl = myAvatarUrl;
        }
        
        // Set player 1 (left/winner) data
        player1Name.setText(winnerName);
        player1Score.setText(String.valueOf(winnerScore));
        try {
            player1Avatar.setImage(new Image(winnerAvatarUrl, true));
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load winner avatar: " + e.getMessage());
            player1Avatar.setImage(new Image("http://localhost:8080/static/avatars/default_avatar.png", true));
        }
        
        // Set rank points for player 1 (winner - positive, green)
        // winnerRankPoints is already positive from server
        String rankPoints1Text = "+" + winnerRankPoints;
        player1RankPoints.setText(rankPoints1Text);
        player1RankPoints.getStyleClass().removeAll("positive", "negative");
        player1RankPoints.getStyleClass().add("positive");
        // Force style to ensure override (size, weight, color)
        player1RankPoints.setStyle("-fx-text-fill: #00CC00; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        // Set player 2 (right/loser) data
        player2Name.setText(loserName);
        player2Score.setText(String.valueOf(loserScore));
        try {
            player2Avatar.setImage(new Image(loserAvatarUrl, true));
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load loser avatar: " + e.getMessage());
            player2Avatar.setImage(new Image("http://localhost:8080/static/avatars/default_avatar.png", true));
        }
        
        // Set rank points for player 2 (loser - negative, red)
        // loserRankPoints from server is positive, but displayed as negative
        String rankPoints2Text = "-" + loserRankPoints;
        player2RankPoints.setText(rankPoints2Text);
        player2RankPoints.getStyleClass().removeAll("positive", "negative");
        player2RankPoints.getStyleClass().add("negative");
        // Force style to ensure override (size, weight, color)
        player2RankPoints.setStyle("-fx-text-fill: #FF0000; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        // Load sword icon
        try {
            swordIcon.setImage(new Image(getClass().getResourceAsStream("/com/example/memorygame/assets/images/icon/sword.png")));
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to load sword icon: " + e.getMessage());
        }
        
        // Button handlers
        rematchButton.setOnAction(e -> {
            // Remove popup
            gameContainer.getChildren().remove(popupRoot);
            
            // Navigate to RoomScreen (waiting lobby)
            try {
                Stage stage = (Stage) gameContainer.getScene().getWindow();
                
                RoomScreenController roomController = new RoomScreenController();
                Scene roomScene = new Scene(roomController.getScreen().getRoot());
                roomScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/RoomScreenStyle.css").toExternalForm());
                
                stage.setScene(roomScene);
                stage.setTitle("Memory Game - Room");
                stage.setResizable(true);
                stage.show();
                System.out.println("[GameScreen] Navigated back to room waiting screen (rematch)");
            } catch (Exception ex) {
                System.err.println("[GameScreen] Failed to navigate to room screen: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        leaveButton.setOnAction(e -> {
            // Remove popup
            gameContainer.getChildren().remove(popupRoot);
            
            // Leave room: call REST to exit (defensive) then go to MainScreen
            try {
                com.example.memorygame.model.user.UserSummary me = com.example.memorygame.utils.UserApi.getCurrentUser();
                if (me != null && gameSettings != null && gameSettings.getRoomId() != null) {
                    boolean ok = com.example.memorygame.utils.RoomApi.exitRoom(gameSettings.getRoomId(), me.id);
                    System.out.println("[GameScreen] Leave room via REST => " + ok);
                }
            } catch (Exception ignore) {}
            
            try {
                Stage stage = (Stage) gameContainer.getScene().getWindow();
                
                MainScreenController mainController = new MainScreenController();
                Scene mainScene = new Scene(mainController.getScreen().getRoot());
                mainScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
                
                stage.setScene(mainScene);
                stage.setTitle("Memory Matching Game");
                stage.show();
                System.out.println("[GameScreen] Navigated back to main screen after game end");
            } catch (Exception ex) {
                System.err.println("[GameScreen] Failed to navigate back: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        // Add popup to gameContainer (on top)
        gameContainer.getChildren().add(popupRoot);
        System.out.println("[GameScreen] Custom game result popup shown");
    }
    
    private void showGameEndAlert(boolean iWon, String message) {
        // Fallback: Show end game dialog (old Alert method)
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        
        if (iWon) {
            alert.setHeaderText("Chúc mừng! Bạn đã chiến thắng!");
        } else {
            alert.setHeaderText("Game Over");
        }
        
        alert.setContentText(message);
        
        // Buttons: Leave room (back to Main) or Rematch (go to Room waiting screen)
        javafx.scene.control.ButtonType leaveBtn = new javafx.scene.control.ButtonType("Rời phòng", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        javafx.scene.control.ButtonType rematchBtn = new javafx.scene.control.ButtonType("Đấu lại", javafx.scene.control.ButtonBar.ButtonData.RIGHT);
        alert.getButtonTypes().setAll(leaveBtn, rematchBtn);
        
        System.out.println("[GameScreen] Showing GAME_END dialog with options");
        
        java.util.Optional<javafx.scene.control.ButtonType> chosen = alert.showAndWait();
        if (chosen.isPresent() && chosen.get() == rematchBtn) {
            // Navigate to RoomScreen (waiting lobby)
            try {
                Stage stage = (Stage) gameContainer.getScene().getWindow();
                
                RoomScreenController roomController = new RoomScreenController();
                Scene roomScene = new Scene(roomController.getScreen().getRoot());
                roomScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/RoomScreenStyle.css").toExternalForm());
                
                stage.setScene(roomScene);
                stage.setTitle("Memory Game - Room");
                stage.setResizable(true);
                stage.show();
                System.out.println("[GameScreen] Navigated back to room waiting screen (rematch)");
            } catch (Exception ex) {
                System.err.println("[GameScreen] Failed to navigate to room screen: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            // Leave room: call REST to exit (defensive) then go to MainScreen
            try {
                com.example.memorygame.model.user.UserSummary me = com.example.memorygame.utils.UserApi.getCurrentUser();
                if (me != null && gameSettings != null && gameSettings.getRoomId() != null) {
                    boolean ok = com.example.memorygame.utils.RoomApi.exitRoom(gameSettings.getRoomId(), me.id);
                    System.out.println("[GameScreen] Leave room via REST => " + ok);
                }
            } catch (Exception ignore) {}
            
            try {
                Stage stage = (Stage) gameContainer.getScene().getWindow();
                
                MainScreenController mainController = new MainScreenController();
                Scene mainScene = new Scene(mainController.getScreen().getRoot());
                mainScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
                
                stage.setScene(mainScene);
                stage.setTitle("Memory Matching Game");
                stage.show();
                System.out.println("[GameScreen] Navigated back to main screen after game end");
            } catch (Exception ex) {
                System.err.println("[GameScreen] Failed to navigate back: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void scheduleOpponentLeftFallback() {
        // If GAME_END does not arrive shortly, synthesize an end dialog
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            if (!gameEndReceived) {
                gameEndReceived = true; // Set flag to prevent duplicate processing
                System.out.println("[GameScreen] Fallback GAME_END due to opponent left signal");
                Map<String, Object> fake = new java.util.HashMap<>();
                fake.put("player1Score", player1Score);
                fake.put("player2Score", player2Score);
                // Prefer winnerId to avoid username/displayName mismatch
                try {
                    com.example.memorygame.model.user.UserSummary me = com.example.memorygame.utils.UserApi.getCurrentUser();
                    if (me != null) {
                        fake.put("winnerId", me.id);
                    } else {
                        fake.put("winnerId", null);
                    }
                } catch (Exception ignore) { fake.put("winnerId", null); }
                String myName = gameSettings != null ? (gameSettings.isHost() ? gameSettings.getPlayer1Name() : gameSettings.getPlayer2Name()) : "Me";
                fake.put("winnerUsername", myName); // I am winner by surrender
                fake.put("loserId", null);
                fake.put("isSurrender", true);
                // Compute rank points locally for display consistency
                int myPairs = (gameSettings != null && gameSettings.isHost()) ? player1Score : player2Score;
                int oppPairs = (gameSettings != null && gameSettings.isHost()) ? player2Score : player1Score;
                fake.put("winnerRankPoints", computeWinnerRankPointsLocal(myPairs, oppPairs));
                fake.put("loserRankPoints", 100);
                Platform.runLater(() -> handleGameEnd(fake));
            } else {
                System.out.println("[GameScreen] GAME_END already received, skipping fallback");
            }
        }).start();
    }

    private int computeWinnerRankPointsLocal(int winnerPairs, int loserPairs) {
        if (loserPairs < 0) loserPairs = 0;
        if (winnerPairs < 0) winnerPairs = 0;
        double ratio = ((double) (winnerPairs + 2)) / (loserPairs + 1);
        return (int) Math.floor(ratio * 10.0);
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
            
            // Start timeout thread - if server doesn't respond in 3 seconds, flip back automatically
            startMatchCheckTimeout();
        } catch (Exception e) {
            System.err.println("[GameScreen] Failed to send cards for match check: " + e.getMessage());
            isResolving = false;
        }
    }
    
    private void startMatchCheckTimeout() {
        // Cancel previous timeout if exists
        if (matchCheckTimeoutThread != null && matchCheckTimeoutThread.isAlive()) {
            matchCheckTimeoutThread.interrupt();
        }
        
        matchCheckTimeoutThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds for server response
                // If we reach here, server didn't respond - flip back the cards
                Platform.runLater(() -> {
                    if (isResolving && flippedCardsCount >= 2) {
                        System.out.println("[GameScreen] Match check timeout - server didn't respond, flipping back cards");
                        handleMatchTimeout();
                    }
                });
            } catch (InterruptedException e) {
                // Timeout was cancelled - server responded, do nothing
                Thread.currentThread().interrupt();
            }
        });
        matchCheckTimeoutThread.start();
    }
    
    private void handleMatchTimeout() {
        // Server didn't respond - flip back the cards as they don't match
        System.out.println("[GameScreen] Handling match timeout - flipping back cards");
        
        if (firstFlippedCard != null && firstFlippedCard.isFlipped()) {
            firstFlippedCard.flipToBack();
            System.out.println("[GameScreen] Flipped back first card due to timeout");
        }
        if (secondFlippedCard != null && secondFlippedCard.isFlipped()) {
            secondFlippedCard.flipToBack();
            System.out.println("[GameScreen] Flipped back second card due to timeout");
        }
        
        resetFlippedCards();
        isResolving = false;
        
        // Switch turn since cards don't match
        isMyTurn = !isMyTurn;
        updateTurnIndicator();
        resetTurnTimer();
        System.out.println("[GameScreen] Match timeout - flipped back and switched turn");
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
        // Cancel timeout thread if exists
        if (matchCheckTimeoutThread != null && matchCheckTimeoutThread.isAlive()) {
            matchCheckTimeoutThread.interrupt();
        }
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
    
    private void updateTurnIndicator() {
        if (turnIndicatorLabel != null) {
            if (isMyTurn) {
                turnIndicatorLabel.setText("Your turn");
            } else {
                turnIndicatorLabel.setText("Opponent's turn");
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
            System.out.println("[GameScreen] Timer ended - switching turn");
            
            // Reset any flipped cards if timer ended during a flip
            if (flippedCardsCount > 0) {
                System.out.println("[GameScreen] Timer ended with cards flipped - flipping back");
                // Flip back any flipped cards
                if (firstFlippedCard != null && firstFlippedCard.isFlipped()) {
                    firstFlippedCard.flipToBack();
                }
                if (secondFlippedCard != null && secondFlippedCard.isFlipped()) {
                    secondFlippedCard.flipToBack();
                }
                resetFlippedCards();
                isResolving = false;
            }
            
            // Switch turn
            isMyTurn = false;
            updateTurnIndicator();
            
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
        
        // Reset timer for the new turn (will start counting down for opponent)
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
