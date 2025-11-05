package com.example.memorygame.view;

import com.example.memorygame.controller.LeaderboardScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class LeaderboardScreen {
    private final Parent root;
    private final LeaderboardScreenController controller;

    public LeaderboardScreen(LeaderboardScreenController controller) throws IOException {
        this.controller = controller;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/LeaderboardScreen.fxml"));
        loader.setController(controller);
        this.root = loader.load();
    }

    public Parent getRoot() {
        return root;
    }

    public LeaderboardScreenController getController() {
        return controller;
    }
}

