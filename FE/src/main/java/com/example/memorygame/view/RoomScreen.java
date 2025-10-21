package com.example.memorygame.view;

import com.example.memorygame.controller.RoomScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class RoomScreen {
    private final Parent root;

    public RoomScreen(RoomScreenController controller) {
        try {
            FXMLLoader loader = new FXMLLoader(RoomScreen.class.getResource("/com/example/memorygame/RoomScreen.fxml"));
            loader.setController(controller);
            this.root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load RoomScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }
}


