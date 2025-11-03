package com.example.memorygame.view;

import com.example.memorygame.controller.AuthScreenController;
import com.example.memorygame.controller.MainScreenController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthScreen {
    @FXML private PasswordField passwordField;
    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Button forgetPasswordButton;
    @FXML private Button signUpButton;
    @FXML private Button googleLoginButton;

    // Swap-mode controls
    @FXML private javafx.scene.layout.VBox loginForm;
    @FXML private javafx.scene.layout.Pane leftBanner;
    @FXML private javafx.scene.layout.Pane rightBanner;
    @FXML private javafx.scene.layout.VBox signupForm;
    @FXML private TextField signupUsernameField;
    @FXML private PasswordField signupPasswordField;
    @FXML private TextField signupEmailField;
    @FXML private Button submitSignUpButton;
    @FXML private Button loginLinkButton;

    private final FXMLLoader loader;

    public AuthScreen(AuthScreenController controller) {
        try {
            this.loader = new FXMLLoader(AuthScreen.class.getResource("/com/example/memorygame/AuthScreen.fxml"));
            loader.setController(this);
            loader.load();
            configureScreenComponentEventHandler(controller);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load AuthScreen.fxml", exception);
        }
    }

    public void refreshData() {
        usernameField.setText("");
        passwordField.setText("");
    }

    private void configureScreenComponentEventHandler(AuthScreenController controller) {
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int loginStatus = controller.handleLogin();
                if (loginStatus == AuthScreenController.LOGIN_SUCCESSFUL) {
                    showMainScreen();
                }
            }
        });
        signUpButton.setOnAction(e -> showSignUp());
        googleLoginButton.setOnAction(e -> controller.handleGoogleLogin());
        forgetPasswordButton.setOnAction(e -> controller.handleForgetPassword());
        if (submitSignUpButton != null) {
            submitSignUpButton.setOnAction(e -> {
                String u = signupUsernameField.getText();
                String p = signupPasswordField.getText();
                String em = signupEmailField.getText();
                boolean ok = controller.handleSignUpSubmit(u, p, em);
                if (ok) {
                    showLogin();
                    usernameField.setText(u);
                }
            });
        }
        if (loginLinkButton != null) {
            loginLinkButton.setOnAction(e -> showLogin());
        }
    }

    public PasswordField getPasswordField() { return passwordField; }
    public TextField getUsernameField() { return usernameField; }
    public <T> T getRoot() { return loader.getRoot(); }

    public void showSignUp() {
        if (loginForm != null) { loginForm.setVisible(false); loginForm.setManaged(false); }
        if (leftBanner != null) { leftBanner.setVisible(true); leftBanner.setManaged(true); }
        if (rightBanner != null) { rightBanner.setVisible(false); rightBanner.setManaged(false); }
        if (signupForm != null) { signupForm.setVisible(true); signupForm.setManaged(true); }
    }

    public void showLogin() {
        if (loginForm != null) { loginForm.setVisible(true); loginForm.setManaged(true); }
        if (leftBanner != null) { leftBanner.setVisible(false); leftBanner.setManaged(false); }
        if (rightBanner != null) { rightBanner.setVisible(true); rightBanner.setManaged(true); }
        if (signupForm != null) { signupForm.setVisible(false); signupForm.setManaged(false); }
    }

    public void showMainScreen() {
        try {
            Stage stage = (Stage) ((Node) this.getRoot()).getScene().getWindow();
            MainScreenController controller = new MainScreenController();
            Parent root = controller.getScreen().getRoot();

            Scene scene = root.getScene() == null ? new Scene(root) : root.getScene();
            // Add MainScreen CSS stylesheet
            scene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
            stage.setResizable(true);
            stage.setTitle("Memory Matching Game");
            stage.setScene(scene);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();
            stage.setX((screenWidth - stage.getWidth()) / 2);
            stage.setY((screenHeight - stage.getHeight()) / 2);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


