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
import com.example.memorygame.utils.ApiClient;
import com.example.memorygame.model.game.ThemeDTO;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.ThemeApi;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.view.RoomScreen;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller for RoomScreen - delegates to helper classes
 */
public class RoomScreenController {
    private final RoomScreen screen;
    
    // Helper classes
    private RoomStateManager stateManager;
    private RoomUIUpdater uiUpdater;
    
    // FXML Components
    @FXML private VBox listContainer;
    @FXML private HBox searchContainer;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private ToggleButton tabFriends;
    @FXML private ToggleButton tabStrangers;
    @FXML private ToggleButton tabRecent;
    @FXML private Label titleLabel;
    @FXML private ImageView hostAvatar;
    @FXML private ImageView guestAvatar;
    @FXML private Label hostStatus;
    @FXML private Label guestStatus;
    @FXML private Label guestPlaceholder;
    @FXML private StackPane playButton;
    @FXML private StackPane questionButton;
    @FXML private StackPane popupOverlay;
    @FXML private Button closePopupButton;
    @FXML private VBox inviteListContainer;
    @FXML private VBox inviteList;
    @FXML private ComboBox<ThemeDTO> themeComboBox;
    @FXML private ComboBox<String> sizeComboBox;
    @FXML private ComboBox<String> timeComboBox;
    
    // Labels for guest view (read-only)
    @FXML private Label themeLabel;
    @FXML private Label sizeLabel;
    @FXML private Label timeLabel;

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
                getClass().getResourceAsStream("/com/example/memorygame/assets/fonts/PlaywriteDESAS-VariableFont_wght.ttf"), 
                26
            );
            if (loaded != null && titleLabel != null) {
                String fam = loaded.getFamily();
                titleLabel.setFont(Font.font(fam, FontWeight.BOLD, 26));
                titleLabel.setStyle("-fx-font-family: '" + fam + "'; -fx-font-size: 26px; -fx-font-weight: bold;");
            }
        } catch (Exception ignored) { }
    }
    
    private void initializeHelpers() {
        // State manager
        this.stateManager = new RoomStateManager();
        
        // UI updater
        this.uiUpdater = new RoomUIUpdater(
                hostAvatar, guestAvatar,
                hostStatus, guestStatus, guestPlaceholder,
                playButton,
                this::loadUserAvatarOrFallback
        );
        
        // Room manager
        roomManager = new RoomManager(stateManager, this::showAlert);
        
        // Friend item builder
        FriendItemBuilder friendItemBuilder = new FriendItemBuilder(
                stateManager,
                this::loadUserAvatarOrFallback,
                roomManager::handleInviteUser
        );
        
        // Friend list manager
        friendListManager = new FriendListManager(
            listContainer, searchContainer, txtSearch,
            tabFriends, tabStrangers, tabRecent,
                friendItemBuilder
        );
        
        // Invite item builder
        inviteItemBuilder = new InviteItemBuilder(
            this::loadUserAvatarOrFallback,
            this::handleAcceptInvite,
            this::handleRejectInvite
        );
        
        // TCP handler
        tcpHandler = new TCPMessageHandler(
                uiUpdater,
            friendListManager::refreshCurrentTab,
            this::loadInvites,
                stateManager,
                this::updateComboBoxStates,
                this::updateLabelsFromSettings,
                this::sendCurrentSettingsToGuest
        );
    }
    
    private void setupUI() {
        friendListManager.setupTabs();
        
        if (btnSearch != null) btnSearch.setOnAction(e -> friendListManager.handleSearch());
        if (txtSearch != null) txtSearch.setOnAction(e -> friendListManager.handleSearch());
        
        setupRoomAvatars();
        setupPopup();
        loadThemes();
        setupGameOptions();
        updateComboBoxStates();
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
                
                // Set initial play button state - disable if no guest
                boolean canStart = stateManager.canStartGame();
                System.out.println("[DEBUG] Initial setup - canStartGame: " + canStart + ", currentGuestId: " + stateManager.getCurrentGuestId());
                uiUpdater.setPlayButtonEnabled(canStart);
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
        // Default avatar URL from server using ApiClient base URL
        String serverDefaultAvatarUrl = ApiClient.getBaseUrl() + "/static/avatars/default_avatar.png";
        
        try {
            if (candidateUrl != null) {
                String trimmed = candidateUrl.trim();
                if (!trimmed.isEmpty()) {
                    String lower = trimmed.toLowerCase();
                    
                    // Check if it's already a full URL
                    if (lower.startsWith("http://") || lower.startsWith("https://")) {
                        return new Image(trimmed, true);
                    }
                    
                    // If it's a relative path, prepend base URL
                    if (trimmed.startsWith("/")) {
                        String fullUrl = ApiClient.getBaseUrl() + trimmed;
                        return new Image(fullUrl, true);
                    }
                    
                    // If it doesn't start with /, assume it's a path under /static/
                    String fullUrl = ApiClient.getBaseUrl() + "/static/" + trimmed;
                    return new Image(fullUrl, true);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] Failed to load avatar: " + e.getMessage());
        }
        
        // Fallback to server default avatar
        return new Image(serverDefaultAvatarUrl, true);
    }
    
    private void loadThemes() {
        new Thread(() -> {
            try {
                List<ThemeDTO> themes = ThemeApi.getAllThemes();
                Platform.runLater(() -> {
                    if (themeComboBox != null && themes != null) {
                        themeComboBox.getItems().clear();
                        themeComboBox.getItems().addAll(themes);
                        
                        // Set default selection to first theme
                        if (!themes.isEmpty()) {
                            themeComboBox.setValue(themes.get(0));
                        }
                        
                        // Set cell factory to display theme name
                        themeComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<ThemeDTO>() {
                            @Override
                            protected void updateItem(ThemeDTO theme, boolean empty) {
                                super.updateItem(theme, empty);
                                if (empty || theme == null) {
                                    setText(null);
                                } else {
                                    setText(theme.name);
                                }
                            }
                        });
                        
                        // Set button cell to display theme name
                        themeComboBox.setButtonCell(new javafx.scene.control.ListCell<ThemeDTO>() {
                            @Override
                            protected void updateItem(ThemeDTO theme, boolean empty) {
                                super.updateItem(theme, empty);
                                if (empty || theme == null) {
                                    setText(null);
                                } else {
                                    setText(theme.name);
                                }
                            }
                        });
                        
                        System.out.println("[DEBUG] Loaded " + themes.size() + " themes");
                    }
                });
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to load themes: " + e.getMessage());
            }
        }).start();
    }
    
    private void setupGameOptions() {
        // Setup Size ComboBox
        if (sizeComboBox != null) {
            sizeComboBox.getItems().addAll("5x6", "6x7");
            sizeComboBox.setValue("5x6"); // Default selection
        }
        
        // Setup Time ComboBox
        if (timeComboBox != null) {
            timeComboBox.getItems().addAll("20s", "30s", "40s");
            timeComboBox.setValue("30s"); // Default selection
        }
        
        // Add change listeners for synchronization
        setupComboBoxListeners();
        
        System.out.println("[DEBUG] Game options setup completed");
    }
    
    private void updateComboBoxStates() {
        boolean isHost = stateManager.isHost();
        
        // Show ComboBox for host, Label for guest
        if (themeComboBox != null && themeLabel != null) {
            themeComboBox.setVisible(isHost);
            themeLabel.setVisible(!isHost);
        }
        
        if (sizeComboBox != null && sizeLabel != null) {
            sizeComboBox.setVisible(isHost);
            sizeLabel.setVisible(!isHost);
        }
        
        if (timeComboBox != null && timeLabel != null) {
            timeComboBox.setVisible(isHost);
            timeLabel.setVisible(!isHost);
        }
        
        System.out.println("[DEBUG] ComboBox/Label states updated - isHost: " + isHost);
    }
    
    private void setupComboBoxListeners() {
        // Only host can change settings and sync to guest
        if (themeComboBox != null) {
            themeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (stateManager.isHost() && newVal != null) {
                    syncSettingsToGuest();
                }
            });
        }
        
        if (sizeComboBox != null) {
            sizeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (stateManager.isHost() && newVal != null) {
                    syncSettingsToGuest();
                }
            });
        }
        
        if (timeComboBox != null) {
            timeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (stateManager.isHost() && newVal != null) {
                    syncSettingsToGuest();
                }
            });
        }
        
        System.out.println("[DEBUG] ComboBox listeners setup completed");
    }
    
    private void syncSettingsToGuest() {
        // TODO: Send TCP message to guest with current settings
        // For now, just log the current settings
        String theme = themeComboBox != null && themeComboBox.getValue() != null ? themeComboBox.getValue().name : "Default";
        String size = sizeComboBox != null ? sizeComboBox.getValue() : "5x6";
        String time = timeComboBox != null ? timeComboBox.getValue() : "30s";
        
        System.out.println("[DEBUG] Host changed settings - Theme: " + theme + ", Size: " + size + ", Time: " + time);
        
        // Send TCP message to sync settings to guest
        sendSettingsToGuest(theme, size, time);
    }
    
    private void sendSettingsToGuest(String theme, String size, String time) {
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("theme", theme);
            data.put("size", size);
            data.put("time", time);
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("ROOM_SETTINGS_CHANGED", data, null, null);
            System.out.println("[DEBUG] Creating TCP message: " + message.getType() + " with data: " + data);
            client.sendMessage(message);
            System.out.println("[DEBUG] Sent settings to guest - Theme: " + theme + ", Size: " + size + ", Time: " + time);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send settings to guest: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateLabelsFromSettings(String theme, String size, String time) {
        Platform.runLater(() -> {
            if (themeLabel != null) {
                themeLabel.setText(theme);
            }
            if (sizeLabel != null) {
                sizeLabel.setText(size);
            }
            if (timeLabel != null) {
                timeLabel.setText(time);
            }
            System.out.println("[DEBUG] Labels updated - Theme: " + theme + ", Size: " + size + ", Time: " + time);
        });
    }
    
    private void sendCurrentSettingsToGuest() {
        // Only send if we are the host
        if (!stateManager.isHost()) {
            return;
        }
        
        String theme = themeComboBox != null && themeComboBox.getValue() != null ? themeComboBox.getValue().name : "Default";
        String size = sizeComboBox != null ? sizeComboBox.getValue() : "5x6";
        String time = timeComboBox != null ? timeComboBox.getValue() : "30s";
        
        System.out.println("[DEBUG] Sending current settings to newly joined guest - Theme: " + theme + ", Size: " + size + ", Time: " + time);
        sendSettingsToGuest(theme, size, time);
    }
}

