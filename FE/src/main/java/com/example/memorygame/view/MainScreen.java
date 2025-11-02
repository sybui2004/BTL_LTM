package com.example.memorygame.view;

import com.example.memorygame.controller.MainScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class MainScreen {
    private final Parent root;

    public MainScreen(MainScreenController controller) {
        try {
            FXMLLoader loader = new FXMLLoader(MainScreen.class.getResource("/com/example/memorygame/MainScreen.fxml"));
            loader.setController(controller);
            this.root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load MainScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
