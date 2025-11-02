package com.example.memorygame.view;

import java.io.IOException;

import com.example.memorygame.controller.AuthScreenController;
import com.example.memorygame.controller.RoomScreenController;

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
                    // showWorldChatOnly(); // TEMP: test WorldChat
                    // showPrivateChatOnly(); // TEMP: test PrivateChat
                    // showMatchChatOnly(); // TEMP: test MatchChat
                    showLobbyChatOnly(); // TEMP: test LobbyChat
                    // showMainScreen();
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
            RoomScreenController controller = new RoomScreenController();
            Parent root = controller.getScreen().getRoot();

            Scene scene = root.getScene() == null ? new Scene(root) : root.getScene();
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

    // TEMP: Điều hướng thẳng tới WorldChat.fxml sau khi đăng nhập thành công
    private void showWorldChatOnly() {
        try {
            Stage stage = (Stage) ((Node) this.getRoot()).getScene().getWindow();
            javafx.fxml.FXMLLoader chatLoader = new javafx.fxml.FXMLLoader(
                AuthScreen.class.getResource("/com/example/memorygame/chat/WorldChat.fxml")
            );
            Parent root = chatLoader.load();

            // Set current user cho controller
            Object ctrl = chatLoader.getController();
            if (ctrl instanceof com.example.memorygame.controller.chat.WorldChatController wc) {
                com.example.memorygame.model.user.UserSummary me = 
                    com.example.memorygame.utils.UserApi.getCurrentUser();
                wc.setCurrentUser(me);
            }

            Scene scene = new Scene(root, 1000, 720);
            stage.setResizable(true);
            stage.setTitle("World Chat");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TEMP: Điều hướng thẳng tới PrivateChat.fxml sau khi đăng nhập thành công
    private void showPrivateChatOnly() {
        try {
            Stage stage = (Stage) ((Node) this.getRoot()).getScene().getWindow();
            javafx.fxml.FXMLLoader chatLoader = new javafx.fxml.FXMLLoader(
                AuthScreen.class.getResource("/com/example/memorygame/chat/PrivateChat.fxml")
            );
            Parent root = chatLoader.load();

            // Set current user cho controller
            Object ctrl = chatLoader.getController();
            if (ctrl instanceof com.example.memorygame.controller.chat.PrivateChatController pc) {
                com.example.memorygame.model.user.UserSummary me = 
                    com.example.memorygame.utils.UserApi.getCurrentUser();
                pc.setCurrentUser(me);
            }

            Scene scene = new Scene(root, 800, 600);
            stage.setResizable(true);
            stage.setTitle("Private Chat");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TEMP: Điều hướng thẳng tới MatchChat.fxml sau khi đăng nhập thành công
    private void showMatchChatOnly() {
        try {
            Stage stage = (Stage) ((Node) this.getRoot()).getScene().getWindow();
            javafx.fxml.FXMLLoader chatLoader = new javafx.fxml.FXMLLoader(
                AuthScreen.class.getResource("/com/example/memorygame/chat/MatchChat.fxml")
            );
            Parent root = chatLoader.load();

            // Set current user và setup match context
            Object ctrl = chatLoader.getController();
            if (ctrl instanceof com.example.memorygame.controller.chat.MatchChatController mc) {
                com.example.memorygame.model.user.UserSummary me = 
                    com.example.memorygame.utils.UserApi.getCurrentUser();

                // Gọi API để lấy phòng hiện tại của user
                java.util.List<com.example.memorygame.model.game.RoomResponseDTO> rooms = 
                    com.example.memorygame.utils.RoomApi.getMyActiveRooms();
                Long roomId = com.example.memorygame.utils.RoomApi.pickBestRoomId(rooms);

                // Nếu user không có phòng nào, hiển thị thông báo và quay về main screen
                if (roomId == null) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("No Active Room");
                    alert.setHeaderText("Bạn chưa tham gia phòng nào");
                    alert.setContentText("Vui lòng tạo hoặc tham gia phòng trước khi vào Match Chat.");
                    alert.showAndWait();
                    showMainScreen(); // Chuyển về màn hình chính
                    return;
                }

                String matchId = String.valueOf(roomId);

                // Tạo đối thủ tạm để UI không null (không ảnh hưởng broadcast)
                com.example.memorygame.model.user.UserSummary opponent = new com.example.memorygame.model.user.UserSummary();
                opponent.id = -1L;
                opponent.username = "opponent";
                opponent.displayName = "Opponent";

                mc.setupMatch(matchId, me, opponent);
            }

            Scene scene = new Scene(root, 800, 600);
            stage.setResizable(true);
            stage.setTitle("Match Chat");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TEMP: Điều hướng thẳng tới LobbyChat.fxml sau khi đăng nhập thành công
    private void showLobbyChatOnly() {
        try {
            Stage stage = (Stage) ((Node) this.getRoot()).getScene().getWindow();
            javafx.fxml.FXMLLoader chatLoader = new javafx.fxml.FXMLLoader(
                AuthScreen.class.getResource("/com/example/memorygame/chat/LobbyChat.fxml")
            );
            Parent root = chatLoader.load();

            // Lấy controller và thiết lập lobby dựa trên phòng hiện tại của user
            Object ctrl = chatLoader.getController();
            if (ctrl instanceof com.example.memorygame.controller.chat.LobbyChatController lc) {
                com.example.memorygame.model.user.UserSummary me =
                    com.example.memorygame.utils.UserApi.getCurrentUser();

                // Lấy danh sách phòng đang tham gia của user và chọn phòng phù hợp
                java.util.List<com.example.memorygame.model.game.RoomResponseDTO> rooms =
                    com.example.memorygame.utils.RoomApi.getMyActiveRooms();
                Long roomId = com.example.memorygame.utils.RoomApi.pickBestRoomId(rooms);

                if (roomId == null) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("No Active Room");
                    alert.setHeaderText("Bạn chưa tham gia phòng nào");
                    alert.setContentText("Vui lòng tạo hoặc tham gia phòng trước khi vào Lobby Chat.");
                    alert.showAndWait();
                    showMainScreen();
                    return;
                }

                lc.setupLobby(String.valueOf(roomId), me);
            }

            Scene scene = new Scene(root, 900, 650);
            stage.setResizable(true);
            stage.setTitle("Lobby Chat");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}




