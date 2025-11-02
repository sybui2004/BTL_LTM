package com.example.memorygame.view.chat;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Wrapper that loads the MatchChat FXML and exposes the root node.
 */
public class MatchChatView {
    private final Parent root;

    public MatchChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(MatchChatView.class.getResource("/com/example/memorygame/chat/MatchChat.fxml"));
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load MatchChat.fxml", e);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
