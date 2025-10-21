package com.example.memorygame.controller;

import com.example.memorygame.utils.AuthApi;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.view.AuthScreen;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;

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
                showAlert("Vui lòng nhập tài khoản và mật khẩu", Alert.AlertType.WARNING);
                return LOGIN_ERROR;
            }
            String token = AuthApi.login(username, password);
            TokenManager.getInstance().setToken(token);
            return LOGIN_SUCCESSFUL;
        } catch (Exception e) {
            showAlert("Đăng nhập thất bại. Vui lòng thử lại!", Alert.AlertType.ERROR);
            return LOGIN_ERROR;
        }
    }

    public void handleSignUp() { /* toggle handled in view */ }
    public void handleGoogleLogin() { showAlert("Đăng nhập Google chưa được hỗ trợ.", Alert.AlertType.INFORMATION); }
    public void handleForgetPassword() { showAlert("Quên mật khẩu chưa được hỗ trợ.", Alert.AlertType.INFORMATION); }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setUsername(String value) { this.username.set(value); }
    public void setPassword(String value) { this.password.set(value); }

    public boolean handleSignUpSubmit(String username, String password, String email) {
        if (username == null || username.isBlank()) {
            showAlert("Vui lòng nhập tên tài khoản!", Alert.AlertType.WARNING);
            return false;
        }
        if (password == null || password.length() < 6) {
            showAlert("Mật khẩu phải có ít nhất 6 ký tự!", Alert.AlertType.WARNING);
            return false;
        }
        if (email == null || email.isBlank()) {
            showAlert("Vui lòng nhập email!", Alert.AlertType.WARNING);
            return false;
        }
        boolean ok = AuthApi.register(username, password, email);
        if (ok) {
            showAlert("Tạo tài khoản thành công! Hãy đăng nhập.", Alert.AlertType.INFORMATION);
            return true;
        } else {
            showAlert("Đăng ký thất bại. Tên tài khoản hoặc email có thể đã tồn tại.", Alert.AlertType.ERROR);
            return false;
        }
    }
}


