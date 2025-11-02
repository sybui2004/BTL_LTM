package com.example.memorygame.view.chat;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Wrapper that loads the LobbyChat FXML and exposes the root node.
 */
public class LobbyChatView {
    private final Parent root;

    public LobbyChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(LobbyChatView.class.getResource("/com/example/memorygame/chat/LobbyChat.fxml"));
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load LobbyChat.fxml", e);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
