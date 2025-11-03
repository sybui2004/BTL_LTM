package com.example.memorygame.controller;

import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.view.MainScreen;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MainScreenController {
    
    @FXML private Button enterRoomButton;
    private MainScreen screen;
    
    public MainScreenController() {
        this.screen = new MainScreen(this);
    }
    
    public MainScreen getScreen() {
        return screen;
    }
    
    @FXML
    private void initialize() {
        // Start background music after UI is loaded to avoid module access issues during FXML loading
        javafx.application.Platform.runLater(() -> {
            com.example.memorygame.utils.SoundManager.playBackgroundMusic("game_music_loop.wav");
        });
        
        setupEnterRoomButton();
        setupTCPHandlers();
    }
    
    /**
     * Register TCP message handlers for MainScreen
     * Some room-related messages are ignored here since we're not in a room
     */
    private void setupTCPHandlers() {
        TCPClient client = TCPClient.getInstance();
        
        // Ignore room-related messages when in MainScreen (they're handled in RoomScreen)
        client.onMessage("ROOM_SETTINGS_CHANGED", message -> {
            // Silently ignore - we're not in a room yet
        });
        
        client.onMessage("GAME_STARTED", message -> {
            // Silently ignore - we're not in a room yet
        });
        
        client.onMessage("CARD_FLIPPED", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("CARD_MATCHED", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("TURN_SWITCH", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("CARDS_FLIP_BACK", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("CARDS_FOR_MATCH_CHECK", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("GAME_STATE_SYNC", message -> {
            // Silently ignore - we're not in a game yet
        });
        
        client.onMessage("PLAYER_SURRENDER", message -> {
            // Silently ignore - we're not in a game yet
        });
    }
    
    private void setupEnterRoomButton() {
        if (enterRoomButton != null) {
            enterRoomButton.setOnAction(e -> {
                // Play button sound
                SoundManager.playSound("button.wav");
                handleEnterRoom();
            });
        }
    }
    
    private void handleEnterRoom() {
        try {
            Stage stage = (Stage) enterRoomButton.getScene().getWindow();
            
            // Navigate to RoomScreen
            RoomScreenController roomController = new RoomScreenController();
            Scene roomScene = new Scene(roomController.getScreen().getRoot());
            roomScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/RoomScreenStyle.css").toExternalForm());
            
            stage.setScene(roomScene);
            stage.setTitle("Memory Game - Room");
            stage.setResizable(true);
            stage.show();
            
            System.out.println("[MainScreen] Navigated to room screen");
            
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to navigate to room screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
