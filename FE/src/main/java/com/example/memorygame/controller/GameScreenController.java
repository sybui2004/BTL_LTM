package com.example.memorygame.controller;

import com.example.memorygame.model.game.GameSettings;
import com.example.memorygame.model.game.ThemeDTO;
import com.example.memorygame.model.game.CardDTO;
import com.example.memorygame.controller.game.MemoryCard;
import com.example.memorygame.utils.CardApi;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.view.GameScreen;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.image.ImageView;
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
    
    // UI Components
    @FXML private StackPane gameContainer;
    @FXML private GridPane cardGrid;
    @FXML private ImageView backgroundImage;
    
    private GameScreen screen;
    
    public GameScreenController() {
        // Constructor without parameters - controller will be injected by FXML
        // Get settings from static variable
        this.gameSettings = staticGameSettings;
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
                
                // Flip the card locally first
                if (!memoryCard.isFlipped() && !memoryCard.isMatched()) {
                    memoryCard.flipToFront();
                    System.out.println("[GameScreen] Flipped card locally at position: " + finalRow + "," + finalCol);
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
                    
                    if (cardIndex1 != null && cardIndex2 != null) {
                        javafx.application.Platform.runLater(() -> {
                            markCardsAsMatched(cardIndex1, cardIndex2);
                        });
                    }
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
                System.out.println("[GameScreen] Flipped card at position " + row + "," + col + " from TCP message");
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
