package com.example.memorygame.view;

import com.example.memorygame.controller.ProfileScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class ProfileScreen {
    private final Parent root;
    private final ProfileScreenController controller;

    public ProfileScreen(ProfileScreenController controller) throws IOException {
        this.controller = controller;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/ProfileScreen.fxml"));
        loader.setController(controller);
        this.root = loader.load();
    }

    public Parent getRoot() {
        return root;
    }

    public ProfileScreenController getController() {
        return controller;
    }
}

