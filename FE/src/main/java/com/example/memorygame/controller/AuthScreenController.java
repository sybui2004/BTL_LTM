package com.example.memorygame.controller;

import com.example.memorygame.utils.AuthApi;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.view.AuthScreen;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import java.util.Map;
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

    public AuthScreen getScreen() { return screen; }

    public int handleLogin() { return handleLogin(username.get(), password.get()); }

    public int handleLogin(String username, String password) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                showAlert("Please enter username and password", Alert.AlertType.WARNING);
                return LOGIN_ERROR;
            }
            String token = AuthApi.login(username, password);
            TokenManager.getInstance().setToken(token);

            // Connect to TCP server for real-time updates
            connectToTCP(username, token);

            return LOGIN_SUCCESSFUL;
        } catch (Exception e) {
            showAlert("Login failed. Please try again!", Alert.AlertType.ERROR);
            return LOGIN_ERROR;
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
                        System.out.println("[TCP] User status changed: " + user + " -> " + (online ? "ONLINE" : "OFFLINE"));
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

    public void handleGoogleLogin() { showAlert("Google login is not supported yet.", Alert.AlertType.INFORMATION); }
    public void handleForgetPassword() { showAlert("Forget password is not supported yet.", Alert.AlertType.INFORMATION); }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean handleSignUpSubmit(String username, String password, String email) {
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


