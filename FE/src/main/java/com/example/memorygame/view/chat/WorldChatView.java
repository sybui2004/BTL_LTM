package com.example.memorygame.view.chat;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Simple wrapper that loads the WorldChat FXML and exposes the root node.
 */
public class WorldChatView {
    private final Parent root;

    public WorldChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(WorldChatView.class.getResource("/com/example/memorygame/chat/WorldChat.fxml"));
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WorldChat.fxml", e);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
