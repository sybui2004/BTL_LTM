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
import com.example.memorygame.model.game.GameSettings;
import com.example.memorygame.model.game.ThemeDTO;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.SoundManager;
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
import javafx.stage.Stage;
import javafx.scene.Scene;

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
    @FXML private StackPane backButton;
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
        
        // Start background music after UI is loaded to avoid module access issues during FXML loading
        Platform.runLater(() -> {
            SoundManager.playBackgroundMusic("game_music_loop.wav");
        });
        
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
        roomManager.setOnLoadGuestInfo(this::loadExistingGuestInfo);
        roomManager.setOnLoadHostInfo(this::loadExistingHostInfo);
        
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
                this::sendCurrentSettingsToGuest,
                this::handleGameStart
        );
    }
    
    private void setupUI() {
        friendListManager.setupTabs();
        
        if (btnSearch != null) {
            btnSearch.setOnAction(e -> {
                SoundManager.playSound("button.wav");
                friendListManager.handleSearch();
            });
        }
        if (txtSearch != null) {
            txtSearch.setOnAction(e -> {
                SoundManager.playSound("button.wav");
                friendListManager.handleSearch();
            });
        }
        
        // Setup play button
        if (playButton != null) {
            playButton.setOnMouseClicked(e -> {
                SoundManager.playSound("button.wav");
                handlePlayButton();
            });
        }
        
        // Setup back button
        if (backButton != null) {
            backButton.setOnMouseClicked(e -> {
                SoundManager.playSound("button.wav");
                handleBack();
            });
        }
        
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
            questionButton.setOnMouseClicked(e -> {
                SoundManager.playSound("button.wav");
                showPopup();
            });
        }
        if (closePopupButton != null) {
            closePopupButton.setOnAction(e -> {
                SoundManager.playSound("button.wav");
                hidePopup();
            });
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
    
    private void loadExistingGuestInfo() {
        new Thread(() -> {
            try {
                Long guestId = stateManager.getCurrentGuestId();
                if (guestId != null) {
                    com.example.memorygame.model.user.UserSummary guest = com.example.memorygame.utils.UserApi.getUserById(guestId);
                    if (guest != null) {
                        String displayName = guest.displayName != null && !guest.displayName.isBlank() ? guest.displayName : guest.username;
                        Platform.runLater(() -> {
                            uiUpdater.updateGuestInfo(displayName, guest.avatarUrl);
                            uiUpdater.setPlayButtonEnabled(stateManager.isHost() && stateManager.canStartGame());
                        });
                        System.out.println("[RoomScreen] Loaded guest info: " + displayName);
                    }
                }
            } catch (Exception e) {
                System.err.println("[RoomScreen] Failed to load guest info: " + e.getMessage());
            }
        }).start();
    }
    
    private void loadExistingHostInfo() {
        new Thread(() -> {
            try {
                Long hostId = stateManager.getCurrentHostId();
                if (hostId != null) {
                    com.example.memorygame.model.user.UserSummary host = com.example.memorygame.utils.UserApi.getUserById(hostId);
                    if (host != null) {
                        String displayName = host.displayName != null && !host.displayName.isBlank() ? host.displayName : host.username;
                        Platform.runLater(() -> {
                            uiUpdater.updateHostInfo(displayName, host.avatarUrl);
                            uiUpdater.setPlayButtonEnabled(false); // Guest can't start game
                            
                            // Also show myself as guest
                            com.example.memorygame.model.user.UserSummary currentUser = com.example.memorygame.utils.UserApi.getCurrentUser();
                            if (currentUser != null) {
                                String myDisplayName = currentUser.displayName != null && !currentUser.displayName.isBlank() 
                                    ? currentUser.displayName : currentUser.username;
                                uiUpdater.updateGuestInfo(myDisplayName, currentUser.avatarUrl);
                            }
                            
                            // Update ComboBox states for guest (hide combo boxes)
                            updateComboBoxStates();
                        });
                        System.out.println("[RoomScreen] Loaded host info: " + displayName);
                    }
                }
            } catch (Exception e) {
                System.err.println("[RoomScreen] Failed to load host info: " + e.getMessage());
            }
        }).start();
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
            sizeComboBox.getItems().addAll("2x3", "5x6", "6x7");
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
    
    private void handlePlayButton() {
        System.out.println("[RoomScreen] Play button clicked");
        
        // Check if game can start
        if (!stateManager.canStartGame()) {
            System.err.println("[RoomScreen] Cannot start game - room not ready");
            return;
        }
        
        // Get current game settings
        GameSettings gameSettings = getCurrentGameSettings();
        if (gameSettings == null) {
            System.err.println("[RoomScreen] Failed to get game settings");
            return;
        }
        
        System.out.println("[RoomScreen] Starting game with settings: " + gameSettings);
        
        // Send TCP message to notify guest about game start
        sendGameStartToGuest(gameSettings);
        
        // Navigate to coin flip screen first
        navigateToCoinFlipScreen(gameSettings);
    }
    
    private GameSettings getCurrentGameSettings() {
        try {
            // Get theme
            ThemeDTO theme = themeComboBox != null ? themeComboBox.getValue() : null;
            if (theme == null) {
                System.err.println("[RoomScreen] No theme selected");
                return null;
            }
            
            // Get size
            String size = sizeComboBox != null ? sizeComboBox.getValue() : "5x6";
            if (size == null) {
                size = "5x6"; // Default
            }
            
            // Get time
            String time = timeComboBox != null ? timeComboBox.getValue() : "30s";
            if (time == null) {
                time = "30s"; // Default
            }
            
            // Get player names based on host/guest role
            String player1Name, player2Name;
            if (stateManager.isHost()) {
                // Current user is host
                player1Name = getCurrentUserName();
                player2Name = getOpponentName();
            } else {
                // Current user is guest
                player1Name = getOpponentName(); // Host is player1
                player2Name = getCurrentUserName(); // Guest is player2
            }
            
            GameSettings gameSettings = new GameSettings(theme, size, time);
            gameSettings.setPlayer1Name(player1Name);
            gameSettings.setPlayer2Name(player2Name);
            gameSettings.setHost(stateManager.isHost());
            gameSettings.setRoomId(stateManager.getCurrentRoomId()); // Set room ID for synchronization
            
            return gameSettings;
                    
        } catch (Exception e) {
            System.err.println("[RoomScreen] Error getting game settings: " + e.getMessage());
            return null;
        }
    }
    
    private String getCurrentUserName() {
        try {
            UserSummary currentUser = UserApi.getCurrentUser();
            // Use displayName for display, fall back to username if displayName is null
            if (currentUser != null) {
                return currentUser.displayName != null && !currentUser.displayName.isBlank() ? currentUser.displayName : currentUser.username;
            }
            return "Me";
        } catch (Exception e) {
            System.err.println("[RoomScreen] Failed to get current user name: " + e.getMessage());
            return "Me";
        }
    }
    
    private String getOpponentName() {
        try {
            // Get opponent from room state - depends on whether we're host or guest
            Long opponentId;
            if (stateManager.isHost()) {
                opponentId = stateManager.getCurrentGuestId(); // Host's opponent is the guest
            } else {
                opponentId = stateManager.getCurrentHostId(); // Guest's opponent is the host
            }
            
            if (opponentId != null) {
                UserSummary opponent = UserApi.getUserById(opponentId);
                // Use displayName for display, fall back to username if displayName is null
                if (opponent != null) {
                    return opponent.displayName != null && !opponent.displayName.isBlank() ? opponent.displayName : opponent.username;
                }
            }
            return "Opponent";
        } catch (Exception e) {
            System.err.println("[RoomScreen] Failed to get opponent name: " + e.getMessage());
            return "Opponent";
        }
    }
    
    private void navigateToCoinFlipScreen(GameSettings gameSettings) {
        try {
            // Get current stage
            Stage stage = (Stage) playButton.getScene().getWindow();
            
            // Set game settings for CoinFlipScreenController
            com.example.memorygame.controller.CoinFlipScreenController.setGameSettings(gameSettings);
            
            // Create coin flip screen controller
            com.example.memorygame.controller.CoinFlipScreenController coinFlipController = 
                new com.example.memorygame.controller.CoinFlipScreenController();
            
            // Create scene
            Scene coinFlipScene = new Scene(coinFlipController.getScreen().getRoot());
            
            // Apply CSS
            coinFlipScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/CoinFlipScreenStyle.css").toExternalForm());
            
            // Set scene and show
            stage.setScene(coinFlipScene);
            stage.setTitle("Memory Game - Tung Đồng Xu");
            stage.setResizable(true);
            stage.show();
            
            System.out.println("[RoomScreen] Navigated to coin flip screen successfully");
            
        } catch (Exception e) {
            System.err.println("[RoomScreen] Failed to navigate to coin flip screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToGameScreen(GameSettings gameSettings) {
        try {
            // Get current stage
            Stage stage = (Stage) playButton.getScene().getWindow();
            
            // Set game settings for GameScreenController
            GameScreenController.setGameSettings(gameSettings);
            
            // Create game screen controller
            GameScreenController gameController = new GameScreenController();
            
            // Create scene
            Scene gameScene = new Scene(gameController.getScreen().getRoot());
            
            // Apply CSS
            gameScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/GameScreenStyle.css").toExternalForm());
            
            // Set scene and show
            stage.setScene(gameScene);
            stage.setTitle("Memory Game - " + gameSettings.getTheme().name);
            stage.setResizable(true);
            stage.show();
            
            System.out.println("[RoomScreen] Navigated to game screen successfully");
            
        } catch (Exception e) {
            System.err.println("[RoomScreen] Failed to navigate to game screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendGameStartToGuest(GameSettings gameSettings) {
        try {
            TCPClient client = TCPClient.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("theme", gameSettings.getTheme().name);
            data.put("size", gameSettings.getSize());
            data.put("time", gameSettings.getTime());
            data.put("player1Name", gameSettings.getPlayer1Name());
            data.put("player2Name", gameSettings.getPlayer2Name());
            
            TCPClient.TCPMessage message = new TCPClient.TCPMessage("GAME_STARTED", data, null, null);
            System.out.println("[DEBUG] Creating GAME_STARTED message: " + message.getType() + " with data: " + data);
            client.sendMessage(message);
            System.out.println("[DEBUG] Sent game start to guest - Theme: " + gameSettings.getTheme().name + ", Size: " + gameSettings.getSize() + ", Time: " + gameSettings.getTime());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send game start to guest: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleGameStart(String theme, String size, String time, String player1Name, String player2Name) {
        System.out.println("[RoomScreen] Received game start from host - Theme: " + theme + ", Size: " + size + ", Time: " + time);
        System.out.println("[RoomScreen] Player names - Player1: " + player1Name + ", Player2: " + player2Name);
        
        // Create GameSettings for guest
        GameSettings gameSettings = new GameSettings();
        gameSettings.setTheme(findThemeByName(theme));
        gameSettings.setSize(size);
        gameSettings.setTime(time);
        // Use names from host's TCP message
        gameSettings.setPlayer1Name(player1Name);
        gameSettings.setPlayer2Name(player2Name);
        gameSettings.setHost(false); // Guest is not host
        gameSettings.setRoomId(stateManager.getCurrentRoomId()); // Set room ID for synchronization
        
        // Navigate to coin flip screen first (same as host)
        navigateToCoinFlipScreen(gameSettings);
    }
    
    private void handleBack() {
        System.out.println("[RoomScreen] Back button clicked - exiting room");
        
        new Thread(() -> {
            try {
                // Get current user and room ID
                com.example.memorygame.model.user.UserSummary currentUser = com.example.memorygame.utils.UserApi.getCurrentUser();
                Long roomId = stateManager.getCurrentRoomId();
                
                if (currentUser != null && roomId != null) {
                    // Exit the room
                    boolean exitSuccess = com.example.memorygame.utils.RoomApi.exitRoom(roomId, currentUser.id);
                    System.out.println("[RoomScreen] Exit room result: " + exitSuccess);
                }
                
                // Navigate to main screen on JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        Stage stage = (Stage) backButton.getScene().getWindow();
                        
                        // Navigate back to MainScreen
                        MainScreenController mainController = new MainScreenController();
                        Scene mainScene = new Scene(mainController.getScreen().getRoot());
                        mainScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
                        
                        stage.setScene(mainScene);
                        stage.setTitle("Memory Matching Game");
                        stage.show();
                        
                        System.out.println("[RoomScreen] Navigated back to main screen");
                        
                    } catch (Exception e) {
                        System.err.println("[RoomScreen] Failed to navigate back: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
            } catch (Exception e) {
                System.err.println("[RoomScreen] Error handling back button: " + e.getMessage());
                e.printStackTrace();
                // Still try to navigate even if exit fails
                Platform.runLater(() -> {
                    try {
                        Stage stage = (Stage) backButton.getScene().getWindow();
                        MainScreenController mainController = new MainScreenController();
                        Scene mainScene = new Scene(mainController.getScreen().getRoot());
                        mainScene.getStylesheets().add(getClass().getResource("/com/example/memorygame/MainScreenStyle.css").toExternalForm());
                        stage.setScene(mainScene);
                        stage.setTitle("Memory Matching Game");
                        stage.show();
                    } catch (Exception ex) {
                        System.err.println("[RoomScreen] Failed to navigate: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }
    
    private ThemeDTO findThemeByName(String themeName) {
        if (themeComboBox != null) {
            for (ThemeDTO theme : themeComboBox.getItems()) {
                if (theme.name.equals(themeName)) {
                    return theme;
                }
            }
        }
        // Fallback - create a basic theme
        ThemeDTO fallbackTheme = new ThemeDTO();
        fallbackTheme.name = themeName;
        fallbackTheme.assetPath = "/static/themes/" + themeName;
        return fallbackTheme;
    }
}

