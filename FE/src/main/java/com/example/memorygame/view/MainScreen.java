package com.example.memorygame.view;

import com.example.memorygame.controller.MainScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class MainScreen {
    private final Parent root;
    private final MainScreenController controller;

    public MainScreen(MainScreenController controller) throws IOException {
        this.controller = controller;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/MainScreen.fxml"));
        loader.setController(controller);
        this.root = loader.load();
    }

    public Parent getRoot() {
        return root;
    }

    public MainScreenController getController() {
        return controller;
    }
}
