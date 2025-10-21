package com.example.memorygame;

import com.example.memorygame.controller.AuthScreenController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FXMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        AuthScreenController authController = new AuthScreenController();
        Scene scene = new Scene(authController.getScreen().getRoot());
        stage.setTitle("Memory Matching Game");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
