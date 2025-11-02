package com.example.memorygame.controller;

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
        setupEnterRoomButton();
    }
    
    private void setupEnterRoomButton() {
        if (enterRoomButton != null) {
            enterRoomButton.setOnAction(e -> handleEnterRoom());
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
