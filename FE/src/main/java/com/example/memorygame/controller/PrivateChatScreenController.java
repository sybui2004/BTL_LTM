package com.example.memorygame.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class PrivateChatScreenController implements javafx.fxml.Initializable {
    @FXML
    private VBox messageContainer;
    @FXML
    private TextField txtInput;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnSticker;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Chat Controller initialized. Ready to load chat history.");
    }

    @FXML
    private void handleSendButton() {
        String message = txtInput.getText();
        if (message != null && !message.trim().isEmpty()) {
            System.out.println("Sending message: " + message);
            
            txtInput.clear();
        }
    }
     
    @FXML
    private void handleStickerButton() {
        System.out.println("Sticker button clicked. Showing sticker chooser...");
    }
}
