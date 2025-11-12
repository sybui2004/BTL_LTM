package com.example.memorygame.view;

import com.example.memorygame.controller.RoomScreenController;
import com.example.memorygame.controller.chat.LobbyChatController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;

import java.io.IOException;

public class RoomScreen {
    private final Parent root;
    private final FXMLLoader loader;

    public RoomScreen(RoomScreenController controller) {
        try {
            this.loader = new FXMLLoader(RoomScreen.class.getResource("/com/example/memorygame/RoomScreen.fxml"));
            loader.setController(controller);
            this.root = loader.load();
            
            // Store lobby chat controller in the included node for easy access
            try {
                Node lobbyChatNode = (Node) loader.getNamespace().get("lobbyChat");
                if (lobbyChatNode != null) {
                    // Get the controller from the included FXML loader
                    Object lobbyController = loader.getNamespace().get("lobbyChatController");
                    if (lobbyController instanceof LobbyChatController) {
                        lobbyChatNode.getProperties().put("controller", lobbyController);
                    }
                }
            } catch (Exception e) {
                System.err.println("[RoomScreen] Failed to store lobby chat controller: " + e.getMessage());
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load RoomScreen.fxml", exception);
        }
    }

    public Parent getRoot() {
        return root;
    }
    
    public FXMLLoader getLoader() {
        return loader;
    }
}


