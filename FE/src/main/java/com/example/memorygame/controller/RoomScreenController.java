package com.example.memorygame.controller;

import com.example.memorygame.controller.friend.FriendItemBuilder;
import com.example.memorygame.controller.friend.FriendListManager;
import com.example.memorygame.controller.room.InviteItemBuilder;
import com.example.memorygame.controller.room.RoomManager;
import com.example.memorygame.controller.room.RoomStateManager;
import com.example.memorygame.controller.room.RoomUIUpdater;
import com.example.memorygame.controller.room.TCPMessageHandler;
import com.example.memorygame.model.game.InviteDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.view.RoomScreen;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Objects;

/**
 * Main controller for RoomScreen - delegates to helper classes
 */
public class RoomScreenController {
    private final RoomScreen screen;

    // FXML Components
    @FXML
    private VBox listContainer;
    @FXML
    private HBox searchContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnSearch;
    @FXML
    private ToggleButton tabFriends;
    @FXML
    private ToggleButton tabStrangers;
    @FXML
    private ToggleButton tabRecent;
    @FXML
    private Label titleLabel;
    @FXML
    private ImageView hostAvatar;
    @FXML
    private ImageView guestAvatar;
    @FXML
    private Label hostStatus;
    @FXML
    private Label guestStatus;
    @FXML
    private Label guestPlaceholder;
    @FXML
    private StackPane playButton;
    @FXML
    private StackPane questionButton;
    @FXML
    private StackPane popupOverlay;
    @FXML
    private Button closePopupButton;
    @FXML
    private VBox inviteListContainer;
    @FXML
    private VBox inviteList;

    private RoomManager roomManager;
    private FriendListManager friendListManager;
    private InviteItemBuilder inviteItemBuilder;
    private TCPMessageHandler tcpHandler;

    public RoomScreenController() {
        this.screen = new RoomScreen(this);
    }

    public RoomScreen getScreen() {
        return screen;
    }

    @FXML
    private void initialize() {
        setupFont();
        initializeHelpers();
        setupUI();

        // Start room operations
        roomManager.createRoom();
        loadInvites();
        friendListManager.switchTab(FriendListManager.Tab.FRIENDS);
    }

    private void setupFont() {
        try {
            Font loaded = Font.loadFont(
                    getClass().getResourceAsStream(
                            "/com/example/memorygame/assets/fonts/PlaywriteDESAS-VariableFont_wght.ttf"),
                    26);
            if (loaded != null && titleLabel != null) {
                String fam = loaded.getFamily();
                titleLabel.setFont(Font.font(fam, FontWeight.BOLD, 26));
                titleLabel.setStyle("-fx-font-family: '" + fam + "'; -fx-font-size: 26px; -fx-font-weight: bold;");
            }
        } catch (Exception ignored) {
        }
    }

    private void initializeHelpers() {
        // State manager
        // Helper classes
        RoomStateManager stateManager = new RoomStateManager();

        // UI updater
        RoomUIUpdater uiUpdater = new RoomUIUpdater(
                hostAvatar, guestAvatar,
                hostStatus, guestStatus, guestPlaceholder,
                playButton,
                this::loadUserAvatarOrFallback);

        // Room manager
        roomManager = new RoomManager(stateManager, this::showAlert);

        // Friend item builder
        FriendItemBuilder friendItemBuilder = new FriendItemBuilder(
                stateManager,
                this::loadUserAvatarOrFallback,
                roomManager::handleInviteUser);

        // Friend list manager
        friendListManager = new FriendListManager(
                listContainer, searchContainer, txtSearch,
                tabFriends, tabStrangers, tabRecent,
                friendItemBuilder);

        // Invite item builder
        inviteItemBuilder = new InviteItemBuilder(
                this::loadUserAvatarOrFallback,
                this::handleAcceptInvite,
                this::handleRejectInvite);

        // TCP handler
        tcpHandler = new TCPMessageHandler(
                uiUpdater,
                friendListManager::refreshCurrentTab,
                this::loadInvites,
                stateManager);
    }

    private void setupUI() {
        friendListManager.setupTabs();

        if (btnSearch != null)
            btnSearch.setOnAction(e -> friendListManager.handleSearch());
        if (txtSearch != null)
            txtSearch.setOnAction(e -> friendListManager.handleSearch());

        setupRoomAvatars();
        setupPopup();
        tcpHandler.setupListeners();
    }

    private void setupRoomAvatars() {
        new Thread(() -> {
            UserSummary currentUser = UserApi.getCurrentUser();
            Platform.runLater(() -> {
                if (hostAvatar != null) {
                    hostAvatar.setVisible(true);

                    if (currentUser != null && currentUser.avatarUrl != null) {
                        hostAvatar.setImage(loadUserAvatarOrFallback(currentUser.avatarUrl));
                    } else {
                        hostAvatar.setImage(loadUserAvatarOrFallback(null));
                    }

                    Rectangle clip = new Rectangle(96, 96);
                    clip.setArcWidth(12);
                    clip.setArcHeight(12);
                    hostAvatar.setClip(clip);
                }

                if (hostStatus != null) {
                    String displayName = "Player";
                    if (currentUser != null) {
                        if (currentUser.displayName != null && !currentUser.displayName.isBlank()) {
                            displayName = currentUser.displayName;
                        } else if (currentUser.username != null && !currentUser.username.isBlank()) {
                            displayName = currentUser.username;
                        }
                    }
                    hostStatus.setText(displayName);
                }
            });
        }).start();

        if (guestAvatar != null) {
            guestAvatar.setVisible(false);
            Rectangle guestClip = new Rectangle(96, 96);
            guestClip.setArcWidth(12);
            guestClip.setArcHeight(12);
            guestAvatar.setClip(guestClip);
        }
        if (guestStatus != null) {
            guestStatus.setText("Waiting for player...");
        }
    }

    private void setupPopup() {
        if (questionButton != null) {
            questionButton.setOnMouseClicked(e -> showPopup());
        }
        if (closePopupButton != null) {
            closePopupButton.setOnAction(e -> hidePopup());
        }
        if (popupOverlay != null) {
            popupOverlay.setOnMouseClicked(e -> {
                if (e.getTarget() == popupOverlay) {
                    hidePopup();
                }
            });
        }
    }

    private void showPopup() {
        if (popupOverlay != null) {
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);
        }
    }

    private void hidePopup() {
        if (popupOverlay != null) {
            popupOverlay.setVisible(false);
            popupOverlay.setManaged(false);
        }
    }

    private void loadInvites() {
        roomManager.loadInvites(invites -> {
            if (invites == null || invites.isEmpty()) {
                inviteListContainer.setVisible(false);
                inviteListContainer.setManaged(false);
            } else {
                inviteList.getChildren().clear();
                for (InviteDTO invite : invites) {
                    inviteList.getChildren().add(inviteItemBuilder.createInviteItem(invite));
                }
                inviteListContainer.setVisible(true);
                inviteListContainer.setManaged(true);
            }
        });
    }

    private void handleAcceptInvite(InviteDTO invite) {
        roomManager.handleAcceptInvite(invite, this::loadInvites);
    }

    private void handleRejectInvite(InviteDTO invite) {
        roomManager.handleRejectInvite(invite, this::loadInvites);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Image loadUserAvatarOrFallback(String candidateUrl) {
        String fallbackResource = "/com/example/memorygame/assets/images/name.png";
        try {
            if (candidateUrl != null) {
                String trimmed = candidateUrl.trim();
                if (!trimmed.isEmpty()) {
                    String lower = trimmed.toLowerCase();
                    // Check if it's a resource path (starts with /)
                    if (lower.startsWith("/")) {
                        var resourceUrl = getClass().getResource(trimmed);
                        if (resourceUrl != null) {
                            return new Image(resourceUrl.toExternalForm(), true);
                        }
                    } else if (lower.startsWith("http://") || lower.startsWith("https://")) {
                        return new Image(trimmed, true);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        var url = getClass().getResource(fallbackResource);
        if (url == null) {
            // Try alternative path in main directory
            url = getClass().getResource("/com/example/memorygame/name.png");
        }
        return new Image(Objects.requireNonNull(url).toExternalForm(), true);
    }
}
