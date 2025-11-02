package com.example.memorygame.view.chat;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Wrapper that loads the PrivateChat FXML and exposes the root node.
 */
public class PrivateChatView {
    private final Parent root;

    public PrivateChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(PrivateChatView.class.getResource("/com/example/memorygame/chat/PrivateChat.fxml"));
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PrivateChat.fxml", e);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
