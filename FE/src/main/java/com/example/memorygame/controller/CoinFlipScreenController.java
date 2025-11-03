package com.example.memorygame.controller;

import com.example.memorygame.model.game.GameSettings;
import com.example.memorygame.view.CoinFlipScreen;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the Coin Flip Screen
 * Determines who goes first by flipping a coin
 */
public class CoinFlipScreenController {
    
    private CoinFlipScreen screen;
    private GameSettings gameSettings;
    private static GameSettings staticGameSettings;
    
    @FXML private StackPane rootContainer;
    @FXML private ImageView coinImageView;
    @FXML private Label titleLabel;
    @FXML private Label resultLabel;
    @FXML private Label instructionLabel;
    
    private volatile boolean coinResultReceived = false;
    private volatile Integer pendingCoinResult = null; // Store result until animation finishes
    private volatile boolean rotationFinished = false;
    private volatile boolean imageSwapFinished = false;
    private boolean currentCoinIs1 = true; // Track which side is currently shown
    private Thread timeoutThread;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    
    public CoinFlipScreenController() {
        this.gameSettings = staticGameSettings;
    }
    
    public static void setGameSettings(GameSettings settings) {
        staticGameSettings = settings;
    }
    
    public CoinFlipScreen getScreen() {
        if (screen == null) {
            this.screen = new CoinFlipScreen(this);
        }
        return screen;
    }
    
    @FXML
    private void initialize() {
        System.out.println("[CoinFlip] Initializing coin flip screen");
        
        // Background is handled by CSS gradient on root container
        
        // Setup TCP handler for both host and guest to receive coin result from server
        setupTCPHandlerForCoinResult();
        
        // Only host sends request to server (to avoid duplicate requests)
        if (gameSettings != null && gameSettings.isHost()) {
            requestCoinFlipFromServer();
        } else {
            System.out.println("[CoinFlip] Guest - waiting for server to send coin flip result");
        }
        
        startCoinFlip();
        
        // Start timeout thread - if server doesn't respond in 5 seconds, fallback to local random
        startTimeoutFallback();
    }
    
    private void setupTCPHandlerForCoinResult() {
        try {
            com.example.memorygame.utils.TCPClient client = com.example.memorygame.utils.TCPClient.getInstance();
            
            client.onMessage("COIN_FLIP_RESULT", message -> {
                System.out.println("[CoinFlip] ✓ Received COIN_FLIP_RESULT from server: " + message.getData());
                
                // Cancel timeout thread
                if (timeoutThread != null && timeoutThread.isAlive()) {
                    timeoutThread.interrupt();
                }
                
                coinResultReceived = true;
                
                java.util.Map<String, Object> data = message.getData();
                if (data != null) {
                    Integer coinResult = convertToInteger(data.get("coinResult"));
                    if (coinResult != null && (coinResult == 1 || coinResult == 2)) {
                        // Store result, wait for animation to finish before showing
                        pendingCoinResult = coinResult;
                        System.out.println("[CoinFlip] Coin result stored: " + coinResult + ", waiting for animation to finish...");
                        
                        // Check if animation already finished
                        Platform.runLater(() -> {
                            checkAndShowResult();
                        });
                    } else {
                        System.err.println("[CoinFlip] Invalid coinResult received: " + coinResult);
                        // Request again
                        retryCoinFlipRequest();
                    }
                } else {
                    System.err.println("[CoinFlip] COIN_FLIP_RESULT has null data");
                    // Request again
                    retryCoinFlipRequest();
                }
            });
            
            System.out.println("[CoinFlip] TCP handler for COIN_FLIP_RESULT setup completed");
        } catch (Exception e) {
            System.err.println("[CoinFlip] Failed to setup TCP handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void requestCoinFlipFromServer() {
        try {
            com.example.memorygame.utils.TCPClient client = com.example.memorygame.utils.TCPClient.getInstance();
            
            // Check if client is connected
            if (client == null) {
                System.err.println("[CoinFlip] ✗ TCPClient is null!");
                return;
            }
            
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            Long roomId = gameSettings != null ? gameSettings.getRoomId() : null;
            data.put("roomId", roomId);
            
            System.out.println("[CoinFlip] Preparing COIN_FLIP_REQUEST - roomId: " + roomId);
            
            com.example.memorygame.utils.TCPClient.TCPMessage message = 
                new com.example.memorygame.utils.TCPClient.TCPMessage("COIN_FLIP_REQUEST", data, null, null);
            
            System.out.println("[CoinFlip] Message created - type: " + message.getType() + ", data: " + message.getData());
            
            client.sendMessage(message);
            System.out.println("[CoinFlip] ✓ COIN_FLIP_REQUEST sent to server for roomId: " + roomId);
        } catch (Exception e) {
            System.err.println("[CoinFlip] ✗ Failed to send COIN_FLIP_REQUEST to server: " + e.getMessage());
            e.printStackTrace();
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
    
    private void startCoinFlip() {
        // Load initial coin image
        String coin1Path = "/com/example/memorygame/assets/images/coin1.png";
        
        // Start with coin1
        Image initialCoin = new Image(getClass().getResourceAsStream(coin1Path));
        coinImageView.setImage(initialCoin);
        
        // Animate coin flipping
        animateCoinFlip();
        
        // Both host and guest will wait for result from server
        // Server will random and send COIN_FLIP_RESULT to both players
        System.out.println("[CoinFlip] Animation started, waiting for server to random coin flip...");
        instructionLabel.setText("Đang tung đồng xu...");
    }
    
    private void startTimeoutFallback() {
        // Cancel previous timeout if exists
        if (timeoutThread != null && timeoutThread.isAlive()) {
            timeoutThread.interrupt();
        }
        
        timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds for server response
                // If we reach here, server didn't respond
                if (!coinResultReceived) {
                    System.err.println("[CoinFlip] ⚠ Timeout: Server didn't respond after 5 seconds");
                    Platform.runLater(() -> {
                        handleTimeout();
                    });
                }
            } catch (InterruptedException e) {
                // Timeout was cancelled - server responded, do nothing
                Thread.currentThread().interrupt();
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }
    
    private void handleTimeout() {
        if (coinResultReceived) {
            return; // Already received result
        }
        
        if (retryCount < MAX_RETRIES) {
            // Retry requesting from server
            retryCount++;
            System.out.println("[CoinFlip] Retrying coin flip request (attempt " + retryCount + "/" + MAX_RETRIES + ")");
            Platform.runLater(() -> {
                instructionLabel.setText("Đang thử lại... (lần " + retryCount + ")");
            });
            
            if (gameSettings != null && gameSettings.isHost()) {
                requestCoinFlipFromServer();
            }
            
            // Reset timeout
            startTimeoutFallback();
        } else {
            // Max retries reached, show error
            System.err.println("[CoinFlip] ✗ Failed to get coin flip result from server after " + MAX_RETRIES + " retries");
            Platform.runLater(() -> {
                instructionLabel.setText("Lỗi: Không thể kết nối với server");
                resultLabel.setText("Vui lòng thử lại sau");
                // Could show error dialog or go back to room screen
            });
        }
    }
    
    private void retryCoinFlipRequest() {
        if (coinResultReceived) {
            return;
        }
        
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            System.out.println("[CoinFlip] Invalid result received, retrying (attempt " + retryCount + "/" + MAX_RETRIES + ")");
            
            if (gameSettings != null && gameSettings.isHost()) {
                // Wait a bit then retry
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        requestCoinFlipFromServer();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        } else {
            System.err.println("[CoinFlip] ✗ Failed after " + MAX_RETRIES + " retries");
            Platform.runLater(() -> {
                instructionLabel.setText("Lỗi: Không thể nhận kết quả từ server");
            });
        }
    }
    
    private void animateCoinFlip() {
        String coin1Path = "/com/example/memorygame/assets/images/coin1.png";
        String coin2Path = "/com/example/memorygame/assets/images/coin2.png";
        
        Image coin1 = new Image(getClass().getResourceAsStream(coin1Path));
        Image coin2 = new Image(getClass().getResourceAsStream(coin2Path));
        
        // Start with coin1 visible
        coinImageView.setImage(coin1);
        currentCoinIs1 = true;
        
        // Reset flags – we will mark both as finished when the sequence completes
        rotationFinished = false;
        imageSwapFinished = false;
        
        // Build a sequence of card-like flips (same timing as MemoryCard: 300ms + 300ms)
        int flips = 6; // total flips before revealing result (~3.6s)
        SequentialTransition sequence = new SequentialTransition();
        for (int i = 0; i < flips; i++) {
            final Image nextImage = currentCoinIs1 ? coin2 : coin1;
            SequentialTransition oneFlip = createCardFlipLikeTransition(nextImage);
            sequence.getChildren().add(oneFlip);
            currentCoinIs1 = !currentCoinIs1;
        }
        
        sequence.setOnFinished(e -> {
            // Mark both animation flags as finished to reuse existing result sync logic
            rotationFinished = true;
            imageSwapFinished = true;
            System.out.println("[CoinFlip] Flip sequence finished");
            checkAndShowResult();
        });
        
        sequence.play();
        instructionLabel.setText("Đang tung đồng xu...");
    }

    /**
     * Create a single 'card-flip-like' transition for the coin image.
     * Shrinks on X to 0, swaps image, then expands back to 1.
     */
    private SequentialTransition createCardFlipLikeTransition(Image nextImage) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(300), coinImageView);
        shrink.setFromX(1.0);
        shrink.setToX(0.0);
        shrink.setOnFinished(e -> coinImageView.setImage(nextImage));
        
        ScaleTransition expand = new ScaleTransition(Duration.millis(300), coinImageView);
        expand.setFromX(0.0);
        expand.setToX(1.0);
        
        return new SequentialTransition(shrink, expand);
    }
    
    private void checkAndShowResult() {
        // Only show result if BOTH animations finished AND result received
        boolean allAnimationsFinished = rotationFinished && imageSwapFinished;
        
        if (allAnimationsFinished && pendingCoinResult != null) {
            Integer result = pendingCoinResult;
            pendingCoinResult = null; // Clear to avoid showing again
            
            System.out.println("[CoinFlip] ✓ Both animations finished and result ready, showing result: " + result);
            showCoinResult(result);
            
            // Wait then navigate
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Wait 3 seconds to show result
                    Platform.runLater(() -> {
                        navigateToGameScreen(result);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            if (!allAnimationsFinished) {
                System.out.println("[CoinFlip] Waiting for animation to finish... " +
                    "(rotation: " + rotationFinished + ", imageSwap: " + imageSwapFinished + 
                    ", result received: " + (pendingCoinResult != null) + ")");
            }
            if (pendingCoinResult == null) {
                System.out.println("[CoinFlip] Waiting for result from server... " +
                    "(animations finished: rotation=" + rotationFinished + ", imageSwap=" + imageSwapFinished + ")");
            }
        }
    }
    
    private void showCoinResult(int coinResult) {
        String coin1Path = "/com/example/memorygame/assets/images/coin1.png";
        String coin2Path = "/com/example/memorygame/assets/images/coin2.png";
        
        // Show final coin side using the same flip animation if needed
        Image finalCoin = new Image(getClass().getResourceAsStream(coinResult == 1 ? coin1Path : coin2Path));
        boolean desiredIs1 = (coinResult == 1);
        if (desiredIs1 != currentCoinIs1) {
            SequentialTransition finalFlip = createCardFlipLikeTransition(finalCoin);
            finalFlip.setOnFinished(e -> currentCoinIs1 = desiredIs1);
            finalFlip.play();
        } else {
            coinImageView.setImage(finalCoin);
        }
        
        // Get player names
        String playerName;
        if (coinResult == 1) {
            // Host goes first
            playerName = (gameSettings != null && gameSettings.getPlayer1Name() != null) 
                ? gameSettings.getPlayer1Name() 
                : "Chủ phòng";
        } else {
            // Guest goes first
            playerName = (gameSettings != null && gameSettings.getPlayer2Name() != null) 
                ? gameSettings.getPlayer2Name() 
                : "Khách";
        }
        
        // Update result text with player name
        resultLabel.setText(playerName + " được đi trước!");
        instructionLabel.setText("Chuẩn bị bắt đầu...");
        
        System.out.println("[CoinFlip] Showing result - coin: " + coinResult + 
                          ", playerName: " + playerName +
                          ", hostFirstTurn: " + (coinResult == 1));
    }
    
    private void navigateToGameScreen(int coinResult) {
        if (gameSettings == null) {
            System.err.println("[CoinFlip] ERROR: Game settings are null!");
            return;
        }
        
        // Set who goes first based on coin result
        // coinResult == 1 means host goes first, coinResult == 2 means guest goes first
        boolean hostFirstTurn = (coinResult == 1);
        gameSettings.setHostFirstTurn(hostFirstTurn);
        
        System.out.println("[CoinFlip] Coin result: " + coinResult + 
                          ", hostFirstTurn: " + hostFirstTurn);
        System.out.println("[CoinFlip] Navigating to game screen with settings: " + gameSettings);
        
        try {
            // Get current stage
            if (rootContainer == null || rootContainer.getScene() == null) {
                System.err.println("[CoinFlip] Cannot get stage - rootContainer or scene is null");
                return;
            }
            Stage stage = (Stage) rootContainer.getScene().getWindow();
            
            // Set game settings for GameScreenController
            GameScreenController.setGameSettings(gameSettings);
            
            // Create game screen controller
            GameScreenController gameController = new GameScreenController();
            
            // Create scene
            Scene gameScene = new Scene(gameController.getScreen().getRoot());
            
            // Apply CSS
            gameScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/GameScreenStyle.css").toExternalForm());
            
            // Set scene and show
            stage.setScene(gameScene);
            stage.setTitle("Memory Game - " + gameSettings.getTheme().name);
            stage.setResizable(true);
            stage.show();
            
            System.out.println("[CoinFlip] Navigated to game screen successfully");
            
        } catch (Exception e) {
            System.err.println("[CoinFlip] Failed to navigate to game screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

