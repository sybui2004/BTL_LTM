package com.example.memorygame.view;

import com.example.memorygame.controller.CoinFlipScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class CoinFlipScreen {
    private final Parent root;

    public CoinFlipScreen(CoinFlipScreenController controller) {
        try {
            FXMLLoader loader = new FXMLLoader(CoinFlipScreen.class.getResource("/com/example/memorygame/CoinFlipScreen.fxml"));
            loader.setController(controller);
            this.root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load CoinFlipScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }
}

