package com.example.memorygame.view;

import com.example.memorygame.controller.MainScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class MainScreen {
    private final Parent root;
    private final MainScreenController controller;
    private final FXMLLoader loader;

    public MainScreen(MainScreenController controller) throws IOException {
        this.controller = controller;
        this.loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/MainScreen.fxml"));
        loader.setController(controller);
        this.root = loader.load();
    }

    public Parent getRoot() {
        return root;
    }

    public MainScreenController getController() {
        return controller;
    }
    
    public FXMLLoader getLoader() {
        return loader;
    }
}
