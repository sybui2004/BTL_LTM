package com.example.memorygame.view;

import java.io.IOException;

import com.example.memorygame.controller.PrivateChatScreenController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class PrivateChatScreen {
    private final Parent root;
    private final PrivateChatScreenController controller;

    public PrivateChatScreen() {
        try {
            this.controller = new PrivateChatScreenController();
            
            FXMLLoader loader = new FXMLLoader(PrivateChatScreen.class.getResource("/com/example/memorygame/view/PrivateChatScreen.fxml"));

            loader.setController(this.controller);

            this.root = loader.load();
        } catch (IOException exception) {

            throw new RuntimeException("Failed to load PrivateChatScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }

    public PrivateChatScreenController getController() {
        return controller;
    }
}