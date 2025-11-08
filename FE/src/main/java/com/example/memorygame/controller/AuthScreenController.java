package com.example.memorygame.controller;

import java.util.Map;

import com.example.memorygame.utils.AuthApi;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.view.AuthScreen;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AuthScreenController {
    public static final int LOGIN_SUCCESSFUL = 1;
    public static final int LOGIN_ERROR = 2;

    private final AuthScreen screen;
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();

    public AuthScreenController() {
        this.screen = new AuthScreen(this);
        username.bind(screen.getUsernameField().textProperty());
        password.bind(screen.getPasswordField().textProperty());
    }

    public AuthScreen getScreen() {
        return screen;
    }

    public int handleLogin() {
        return handleLogin(username.get(), password.get());
    }

    public int handleLogin(String username, String password) {
        SoundManager.playSound("button.wav");
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                showAlert("Please enter username and password", Alert.AlertType.WARNING);
                return LOGIN_ERROR;
            }
            String token = AuthApi.login(username, password);
            TokenManager.getInstance().setToken(token);

            // Connect to TCP server for real-time updates
            connectToTCP(username, token);

            // Navigate to Main Screen
            navigateToMainScreen();

            return LOGIN_SUCCESSFUL;
        } catch (Exception e) {
            showAlert("Login failed. Please try again!", Alert.AlertType.ERROR);
            return LOGIN_ERROR;
        }
    }

    private void navigateToMainScreen() {
        try {
            // Delay navigation to ensure TCP connection is established
            javafx.application.Platform.runLater(() -> {
                try {
                    MainScreenController mainController = new MainScreenController();
                    
                    // Get the stage from the current scene to preserve size
                    Object root = this.screen.getRoot();
                    if (root instanceof Node) {
                        Node rootNode = (Node) root;
                        Scene currentScene = rootNode.getScene();
                        
                        Stage stage = null;
                        if (currentScene != null) {
                            stage = (Stage) currentScene.getWindow();
                        }
                        
                        // If stage is not available from current scene, try to get it from main controller's scene
                        if (stage == null) {
                            // Try to get stage from any visible window
                            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                                if (window instanceof Stage && window.isShowing()) {
                                    stage = (Stage) window;
                                    break;
                                }
                            }
                        }
                        
                        Scene scene = new Scene(mainController.getScreen().getRoot());
                        
                        if (stage != null) {
                            // Preserve current stage size
                            double currentWidth = stage.getWidth();
                            double currentHeight = stage.getHeight();
                            
                            stage.setScene(scene);
                            
                            // Restore stage size to match AuthScreen
                            if (currentWidth > 0 && currentHeight > 0) {
                                stage.setWidth(currentWidth);
                                stage.setHeight(currentHeight);
                            } else {
                                // Default size if stage size not available
                                stage.setWidth(1024);
                                stage.setHeight(720);
                            }
                            
                            // Center the window
                            stage.centerOnScreen();
                        } else {
                            // If no stage found, create a new one
                            stage = new Stage();
                            stage.setScene(scene);
                            stage.setWidth(1024);
                            stage.setHeight(720);
                            stage.centerOnScreen();
                            stage.show();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Auth] Failed to navigate to Main Screen: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("[Auth] Navigation error: " + e.getMessage());
        }
    }

    private void connectToTCP(String username, String token) {
        new Thread(() -> {
            TCPClient client = TCPClient.getInstance();

            // Register basic handlers before connecting
            client.onMessage("LOGIN_SUCCESS", message -> {
                System.out.println("[TCP] TCP login successful");
            });

            client.onMessage("LOGIN_FAILED", message -> {
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Object reason = data.get("reason");
                    System.err.println("[TCP] Login failed: " + reason);
                }
            });

            // Register USER_STATUS handler (will be used later in RoomScreen)
            client.onMessage("USER_STATUS", message -> {
                Map<String, Object> data = message.getData();
                if (data != null) {
                    Object userObj = data.get("user");
                    Object onlineObj = data.get("online");

                    if (userObj != null && onlineObj != null) {
                        String user = userObj.toString();
                        boolean online = Boolean.parseBoolean(onlineObj.toString());
                        System.out.println(
                                "[TCP] User status changed: " + user + " -> " + (online ? "ONLINE" : "OFFLINE"));
                    }
                }
            });

            boolean connected = client.connect(username, token);
            if (connected) {
                System.out.println("[TCP] Connected successfully");
            } else {
                System.err.println("[TCP] Failed to connect");
            }
        }).start();
    }

    public void handleGoogleLogin() {
        SoundManager.playSound("button.wav");
        showAlert("Google login is not supported yet.", Alert.AlertType.INFORMATION);
    }

    public void handleForgetPassword() {
        SoundManager.playSound("button.wav");
        showAlert("Forget password is not supported yet.", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean handleSignUpSubmit(String username, String password, String email) {
        SoundManager.playSound("button.wav");
        if (username == null || username.isBlank()) {
            showAlert("Please enter username!", Alert.AlertType.WARNING);
            return false;
        }
        if (password == null || password.length() < 6) {
            showAlert("Password must be at least 6 characters!", Alert.AlertType.WARNING);
            return false;
        }
        if (email == null || email.isBlank()) {
            showAlert("Please enter email!", Alert.AlertType.WARNING);
            return false;
        }
        boolean ok = AuthApi.register(username, password, email);
        if (ok) {
            showAlert("Account created successfully! Please login.", Alert.AlertType.INFORMATION);
            return true;
        } else {
            showAlert("Registration failed. Username or email may already exist.", Alert.AlertType.ERROR);
            return false;
        }
    }
}
