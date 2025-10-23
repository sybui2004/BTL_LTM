package com.example.memorygame.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class GameScreen {
    private final Parent root;

    public GameScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(GameScreen.class.getResource("/com/example/memorygame/GameScreen.fxml"));
            this.root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load GameScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
