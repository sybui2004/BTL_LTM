package com.example.memorygame.controller;

import com.example.memorygame.controller.main.FriendListUIManager;
import com.example.memorygame.controller.main.LeaderboardManager;
import com.example.memorygame.controller.main.NotificationManager;
import com.example.memorygame.controller.main.WorldChatManager;
import com.example.memorygame.model.game.LeaderboardRow;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.view.MainScreen;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main Screen Controller - Home/Lobby screen
 */
public class MainScreenController {
    private final MainScreen screen;

    // FXML Components
    @FXML
    private ImageView personalAvatar;
    @FXML
    private Label personalName;
    @FXML
    private Label personalElo;
    @FXML
    private Button startButton;

    // Leaderboard
    @FXML
    private VBox top3Container;
    @FXML
    private ImageView leaderboardRankIcon;

    // Friends (RoomScreen style)
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

    // Chat
    @FXML
    private TextField chatInput;

    // Popups
    @FXML
    private StackPane popupOverlay;
    @FXML
    private VBox helpPopup;
    @FXML
    private VBox settingsPopup;
    @FXML
    private VBox notificationsPopup;
    @FXML
    private HBox leaderboardPopupContainer;
    @FXML
    private BorderPane leaderboardPopup;
    @FXML
    private ToggleButton tabFriendsLeaderboard;
    @FXML
    private ToggleButton tabGlobalLeaderboard;
    @FXML
    private ToggleGroup leaderboardTabGroup;

    // Popup content containers
    @FXML
    private VBox notificationsContainer;
    @FXML
    private TableView<LeaderboardRow> leaderboardTable;
    @FXML
    private TableColumn<LeaderboardRow, String> rankColumn;
    @FXML
    private TableColumn<LeaderboardRow, String> nameColumn;
    @FXML
    private TableColumn<LeaderboardRow, String> eloColumn;
    @FXML
    private TableColumn<LeaderboardRow, String> winsColumn;

    // Settings
    @FXML
    private CheckBox soundEffectsCheckbox;
    @FXML
    private CheckBox backgroundMusicCheckbox;
    @FXML
    private CheckBox notificationsCheckbox;

    // Notification badge
    @FXML
    private Circle notificationBadge;

    // Game banner
    @FXML
    private ImageView gameBanner;
    @FXML
    private javafx.scene.text.Text memoryGameText;
    @FXML
    private StackPane startHexagonWrapper;

    // Helper managers
    private LeaderboardManager leaderboardManager;
    private NotificationManager notificationManager;
    private FriendListUIManager friendListManager;
    private WorldChatManager worldChatManager;

    private UserSummary currentUser;

    public MainScreenController() {
        try {
            this.screen = new MainScreen(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load MainScreen", e);
        }
    }

    public MainScreen getScreen() {
        return screen;
    }

    @FXML
    private void initialize() {
        loadCurrentUser();
        setupGameBanner();
        setupStartButton();
        setupLeaderboardRankIcon();
        setupLeaderboardTable();
        initializeHelpers();
        loadData();
        setupTCPListeners();
    }

    private void setupLeaderboardTable() {
        if (leaderboardTable != null && rankColumn != null && nameColumn != null && eloColumn != null
                && winsColumn != null) {
            // Set column resize policy to prevent extra columns
            leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Setup rank column with custom cell for golden color
            rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
            rankColumn.setCellFactory(column -> new TableCell<LeaderboardRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("leaderboard-rank-top3", "leaderboard-rank-normal");
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                        setEffect(null);
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                        try {
                            int rankNum = Integer.parseInt(item);
                            if (rankNum == 1) {
                                getStyleClass().add("leaderboard-rank-1");
                            } else if (rankNum == 2) {
                                getStyleClass().add("leaderboard-rank-2");
                            } else if (rankNum == 3) {
                                getStyleClass().add("leaderboard-rank-3");
                            } else {
                                getStyleClass().add("leaderboard-rank-normal");
                            }
                            // Không có effect, chỉ hiển thị chữ màu đơn giản
                            setEffect(null);
                        } catch (NumberFormatException e) {
                            getStyleClass().add("leaderboard-rank-normal");
                            setEffect(null);
                        }
                    }
                }
            });

            // Setup name column with avatar + name
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setCellFactory(column -> new TableCell<LeaderboardRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        LeaderboardRow row = getTableView().getItems().get(getIndex());
                        HBox container = new HBox(10);
                        container.setAlignment(Pos.CENTER_LEFT);

                        // Avatar
                        ImageView avatarView = new ImageView();
                        if (row.getAvatar() != null) {
                            avatarView.setImage(row.getAvatar());
                        } else {
                            // Use loadUserAvatarOrFallback method directly
                            Image fallbackAvatar = loadUserAvatarOrFallback(null);
                            avatarView.setImage(fallbackAvatar);
                        }
                        avatarView.setFitWidth(40);
                        avatarView.setFitHeight(40);
                        avatarView.setPreserveRatio(true);
                        Circle clip = new Circle(20, 20, 20);
                        avatarView.setClip(clip);
                        avatarView.getStyleClass().add("leaderboard-avatar");

                        // Name label
                        Label nameLabel = new Label(item);
                        nameLabel.getStyleClass().add("leaderboard-name");
                        nameLabel.setAlignment(Pos.CENTER_LEFT);

                        container.getChildren().addAll(avatarView, nameLabel);
                        setGraphic(container);
                        setText(null);
                    }
                }
            });

            // Setup Elo column - căn giữa
            eloColumn.setCellValueFactory(new PropertyValueFactory<>("elo"));
            eloColumn.setCellFactory(column -> new TableCell<LeaderboardRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                        getStyleClass().add("leaderboard-elo");
                    }
                }
            });

            // Setup Wins column - căn giữa
            winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
            winsColumn.setCellFactory(column -> new TableCell<LeaderboardRow, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                        getStyleClass().add("leaderboard-wins");
                    }
                }
            });

            // Highlight current user row
            leaderboardTable.setRowFactory(tv -> {
                TableRow<LeaderboardRow> row = new TableRow<LeaderboardRow>() {
                    @Override
                    protected void updateItem(LeaderboardRow item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            getStyleClass().remove("leaderboard-row-current");
                        } else {
                            if (currentUser != null && item.getUserId() != null &&
                                    item.getUserId().equals(currentUser.id)) {
                                getStyleClass().add("leaderboard-row-current");
                            } else {
                                getStyleClass().remove("leaderboard-row-current");
                            }
                        }
                    }
                };
                return row;
            });
        }
    }

    private void setupLeaderboardRankIcon() {
        if (leaderboardRankIcon != null) {
            try {
                // Load rank icon from FE resources
                var url = getClass().getResource("/com/example/memorygame/assets/images/icon/rank.png");
                if (url == null) {
                    // Try alternative path in main directory
                    url = getClass().getResource("/com/example/memorygame/rank.png");
                }
                if (url != null) {
                    Image rankImage = new Image(url.toExternalForm());
                    leaderboardRankIcon.setImage(rankImage);
                    leaderboardRankIcon.setFitWidth(36);
                    leaderboardRankIcon.setFitHeight(36);
                    leaderboardRankIcon.setPreserveRatio(true);
                } else {
                    System.err
                            .println("[MainScreen] Rank icon not found. Tried: assets/images/icon/rank.png, rank.png");
                }
            } catch (Exception e) {
                System.err.println("[MainScreen] Failed to load rank icon: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void setupStartButton() {
        // Làm bo góc mềm mại cho nút Start - bo góc đẹp hơn
        if (startHexagonWrapper != null) {
            Platform.runLater(() -> {
                double width = 230; // Match với CSS (230)
                double height = 105; // Match với CSS (105)

                Rectangle roundedClip = new Rectangle(width, height);
                roundedClip.setArcWidth(100); // Bo góc đẹp hơn (từ 90 lên 100)
                roundedClip.setArcHeight(100); // Bo góc đẹp hơn (từ 90 lên 100)
                startHexagonWrapper.setClip(roundedClip);
            });
        }

        // Tạo embossed effect cho START button text
        if (startButton != null) {
            Platform.runLater(() -> {
                // Tạo embossed/3D effect bằng cách kết hợp nhiều drop shadow
                DropShadow highlight = new DropShadow();
                highlight.setColor(javafx.scene.paint.Color.rgb(255, 255, 255, 0.6));
                highlight.setRadius(2);
                highlight.setOffsetX(0.5);
                highlight.setOffsetY(0.5);

                DropShadow shadow = new DropShadow();
                shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.3));
                shadow.setRadius(2);
                shadow.setOffsetX(-0.5);
                shadow.setOffsetY(-0.5);

                // Sử dụng Blend để kết hợp effects (hoặc chỉ dùng một effect chính)
                startButton.setEffect(shadow);
            });
        }
    }

    private void loadCurrentUser() {
        new Thread(() -> {
            try {
                currentUser = UserApi.getCurrentUser();
                Platform.runLater(this::updatePersonalInfo);
            } catch (Exception e) {
                System.err.println("[MainScreen] Failed to load current user: " + e.getMessage());
            }
        }).start();
    }

    private void updatePersonalInfo() {
        if (currentUser != null) {
            // Set avatar - fill entire container
            if (personalAvatar != null) {
                Image avatar = loadUserAvatarOrFallback(currentUser.avatarUrl);
                personalAvatar.setImage(avatar);
                personalAvatar.setFitWidth(100);
                personalAvatar.setFitHeight(100);
                personalAvatar.setPreserveRatio(false);

                // Bo góc avatar (rounded rectangle)
                Rectangle clip = new Rectangle(100, 100);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                personalAvatar.setClip(clip);
            }

            // Set name
            if (personalName != null) {
                String displayName = currentUser.displayName != null && !currentUser.displayName.isBlank()
                        ? currentUser.displayName
                        : currentUser.username;
                personalName.setText(displayName);
            }

            // Set Elo - Get real Elo from API
            if (personalElo != null) {
                new Thread(() -> {
                    try {
                        Integer elo = UserApi.getUserElo(currentUser.id);
                        Platform.runLater(() -> {
                            if (personalElo != null) {
                                personalElo.setText("Elo: " + (elo != null ? elo : 0));
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[MainScreen] Failed to get Elo: " + e.getMessage());
                        Platform.runLater(() -> {
                            if (personalElo != null) {
                                personalElo.setText("Elo: 0");
                            }
                        });
                    }
                }).start();
            }
        }
    }

    private void setupGameBanner() {
        if (gameBanner != null) {
            try {
                // Load game banner image (circular, 560x560) - giống ảnh
                var url = getClass().getResource("/com/example/memorygame/assets/images/mainscreen.png");
                if (url != null) {
                    Image bannerImage = new Image(url.toExternalForm());
                    gameBanner.setImage(bannerImage);
                    gameBanner.setFitWidth(560);
                    gameBanner.setFitHeight(560);
                    gameBanner.setPreserveRatio(true);

                    // Make image circular (giống ảnh - có thể hơi méo một chút nhưng gần như tròn)
                    Circle clip = new Circle(280, 280, 280);
                    gameBanner.setClip(clip);
                } else {
                    System.err.println("[MainScreen] Game banner image not found");
                }
            } catch (Exception e) {
                System.err.println("[MainScreen] Failed to load game banner: " + e.getMessage());
            }
        }

        // Setup gradient text for "MEMORY GAME" - giống ảnh với gradient và white glow
        if (memoryGameText != null) {
            // Create gradient from light blue/cyan to pink/purple (giống ảnh)
            LinearGradient gradient = new LinearGradient(
                    0, 0, 1, 0, // Start at left (0,0), end at right (1,0) - horizontal gradient
                    true, // proportional
                    CycleMethod.NO_CYCLE,
                    new Stop(0.0, javafx.scene.paint.Color.web("#5EC0FF")), // Bright light blue/cyan at start
                    new Stop(0.4, javafx.scene.paint.Color.web("#9370DB")), // Purple in middle
                    new Stop(1.0, javafx.scene.paint.Color.web("#FF69B4")) // Vibrant pink at end
            );
            memoryGameText.setFill(gradient);
            memoryGameText.setFont(Font.font("Arial Black", FontWeight.BOLD, 60));

            // Add prominent white glow/drop shadow effect (giống ảnh - có white glow rõ
            // ràng)
            DropShadow dropShadow = new DropShadow();
            dropShadow.setColor(javafx.scene.paint.Color.WHITE);
            dropShadow.setRadius(16);
            dropShadow.setSpread(0.6);
            dropShadow.setOffsetX(0);
            dropShadow.setOffsetY(0);
            memoryGameText.setEffect(dropShadow);

            // Căn chỉnh text giữa, cao lên một chút
            Platform.runLater(() -> {
                if (memoryGameText != null) {
                    memoryGameText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                }
            });
        }
    }

    private void initializeHelpers() {
        // Leaderboard Manager
        leaderboardManager = new LeaderboardManager(
                top3Container,
                leaderboardTable,
                this::loadUserAvatarOrFallback);
        leaderboardManager.setOnViewProfile(this::handleViewProfile);

        // World Chat Manager - for world chat messaging
        worldChatManager = new WorldChatManager(
                null, // No chat panel UI
                null, // No scroll pane
                currentUser,
                this::loadUserAvatarOrFallback);

        // Notification Manager
        notificationManager = new NotificationManager(
                notificationsContainer,
                notificationBadge,
                this::loadUserAvatarOrFallback);

        // Friend List Manager (RoomScreen style)
        friendListManager = new FriendListUIManager(
                listContainer,
                searchContainer,
                txtSearch,
                tabFriends,
                tabStrangers,
                this::loadUserAvatarOrFallback,
                this::handleViewProfile,
                this::handleOpenChat);

        // Mirror RoomScreen: wire search actions and setup tabs
        if (btnSearch != null)
            btnSearch.setOnAction(e -> friendListManager.handleSearch());
        if (txtSearch != null)
            txtSearch.setOnAction(e -> friendListManager.handleSearch());
        // Default to show friends
        friendListManager.selectFriendsTab();
    }

    private void loadData() {
        leaderboardManager.loadTop3();
        // worldChatManager removed - chat is now just input at bottom
        notificationManager.loadFriendRequests();
        friendListManager.loadFriends();
    }

    private void setupTCPListeners() {
        TCPClient client = TCPClient.getInstance();

        // Listen for world chat messages
        client.onMessage("WORLD_CHAT", message -> {
            if (worldChatManager != null) {
                worldChatManager.handleIncomingMessage(message);
            }
        });

        // Listen for friend status updates
        client.onMessage("USER_STATUS", message -> {
            friendListManager.handleUserStatusChange(message);
        });

        // Listen for friend request notifications (real-time)
        client.onMessage("FRIEND_REQUEST_RECEIVED", message -> {
            System.out.println("[MainScreen] Received friend request notification via TCP");
            if (notificationManager != null) {
                // Refresh notifications to show the new friend request
                notificationManager.refreshNotifications();
            }
        });

        // Listen for friend status changes (accept/reject/remove) - refresh friend list
        client.onMessage("FRIEND_STATUS_CHANGED", message -> {
            System.out.println("[MainScreen] Received friend status changed notification via TCP");
            // Refresh friend list if currently showing friends tab
            if (friendListManager != null) {
                friendListManager.refreshCurrentTab();
            }
            // Also refresh notifications to remove accepted/rejected requests
            if (notificationManager != null) {
                notificationManager.refreshNotifications();
            }
        });

        // Listen for profile updates (display name/avatar) - refresh friend list
        client.onMessage("USER_PROFILE_UPDATED", message -> {
            System.out.println("[MainScreen] Received user profile updated notification via TCP");
            // Refresh friend list to show updated display names and avatars
            if (friendListManager != null) {
                friendListManager.refreshCurrentTab();
            }
        });
    }

    @FXML
    private void handleAvatarClick() {
        try {
            if (currentUser == null) {
                showAlert("Unable to load user information", Alert.AlertType.ERROR);
                return;
            }

            ProfileScreenController profileController = new ProfileScreenController(currentUser.id);
            Scene profileScene = new Scene(profileController.getScreen().getRoot());
            Stage stage = (Stage) personalAvatar.getScene().getWindow();
            Scene currentScene = personalAvatar.getScene();

            // Keep stage size
            // Preserve current stage size (should be 1024x720 to match all screens)
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            if (currentWidth > 0 && currentHeight > 0) {
                stage.setWidth(currentWidth);
                stage.setHeight(currentHeight);
            } else {
                stage.setWidth(1024);
                stage.setHeight(720);
            }

            // Store current scene for later reference
            final Scene mainScene = currentScene;

            // Listen for when we return to MainScreen
            // Use a wrapper class to hold the listener reference
            final class ListenerWrapper {
                javafx.beans.value.ChangeListener<Scene> listener;
            }
            final ListenerWrapper wrapper = new ListenerWrapper();
            wrapper.listener = (obs, oldScene, newScene) -> {
                if (newScene == mainScene && oldScene == profileScene) {
                    // Just returned from ProfileScreen, refresh user info
                    refreshCurrentUserInfo();
                    // Remove listener to prevent memory leak
                    if (wrapper.listener != null) {
                        stage.sceneProperty().removeListener(wrapper.listener);
                    }
                }
            };
            stage.sceneProperty().addListener(wrapper.listener);

            stage.setScene(profileScene);
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to navigate to Profile: " + e.getMessage());
            e.printStackTrace();
            showAlert("Unable to open profile page", Alert.AlertType.ERROR);
        }
    }

    /**
     * Public method to refresh current user info (avatar and display name)
     * Can be called from other controllers or when returning from ProfileScreen
     */
    public void refreshCurrentUserInfo() {
        new Thread(() -> {
            try {
                // Reload current user from API
                currentUser = UserApi.getCurrentUser();
                Platform.runLater(() -> {
                    updatePersonalInfo();
                    // Also refresh friend list to update display names
                    if (friendListManager != null) {
                        friendListManager.refreshCurrentTab();
                    }
                });
            } catch (Exception e) {
                System.err.println("[MainScreen] Failed to refresh current user: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleLeaderboardClick() {
        // Close any other popups first
        closeAllPopups();

        // Show leaderboard as overlay popup - slide from left
        setupLeaderboardTabs();
        leaderboardManager.loadLeaderboard(true); // Load friends by default
        showLeaderboardPopup();
    }

    private void showLeaderboardPopup() {
        if (leaderboardPopupContainer == null || popupOverlay == null)
            return;

        // Set initial position (off-screen to the left)
        leaderboardPopupContainer.setTranslateX(-680);
        leaderboardPopupContainer.setVisible(true);
        leaderboardPopupContainer.setManaged(true);
        popupOverlay.setVisible(true);
        popupOverlay.setManaged(true);

        // Animate slide in from left (leaderboard from left edge)
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), leaderboardPopupContainer);
        slideIn.setToX(0); // Slide to position 0 (left edge of screen)
        slideIn.play();
    }

    private void closeAllPopups() {
        // Close settings and notifications popups
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
            settingsPopup.setManaged(false);
        }
        if (notificationsPopup != null) {
            notificationsPopup.setVisible(false);
            notificationsPopup.setManaged(false);
        }
        if (helpPopup != null) {
            helpPopup.setVisible(false);
        }
        // Note: Don't close overlay here as leaderboard uses it
    }

    @FXML
    private void closeLeaderboard() {
        if (leaderboardPopupContainer == null)
            return;

        // Animate slide out to left
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), leaderboardPopupContainer);
        slideOut.setToX(-680);
        slideOut.setOnFinished(e -> {
            leaderboardPopupContainer.setVisible(false);
            leaderboardPopupContainer.setManaged(false);
            if (popupOverlay != null) {
                popupOverlay.setVisible(false);
                popupOverlay.setManaged(false);
            }
        });
        slideOut.play();
    }

    private void setupLeaderboardTabs() {
        if (leaderboardTabGroup == null || tabFriendsLeaderboard == null || tabGlobalLeaderboard == null)
            return;

        tabFriendsLeaderboard.setToggleGroup(leaderboardTabGroup);
        tabGlobalLeaderboard.setToggleGroup(leaderboardTabGroup);
        tabFriendsLeaderboard.setSelected(true);

        leaderboardTabGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null)
                return;
            if (newT == tabFriendsLeaderboard) {
                leaderboardManager.loadLeaderboard(true); // Friends
            } else {
                leaderboardManager.loadLeaderboard(false); // Global
            }
        });
    }

    @FXML
    private void handleChatInputClick() {
        // Focus vào chat input khi click
        if (chatInput != null) {
            Platform.runLater(() -> chatInput.requestFocus());
        }
    }

    @FXML
    private void handleSendMessage() {
        // Gửi tin nhắn world chat trực tiếp từ input bar
        if (chatInput != null && chatInput.getText() != null) {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                if (worldChatManager != null) {
                    worldChatManager.sendMessage(message);
                }
                chatInput.clear();
            }
        }
    }

    @FXML
    private void handleOverlayClick(javafx.scene.input.MouseEvent event) {
        // Handle overlay clicks for popups (no chat panel anymore)
    }

    @FXML
    private void handleStartGame() {
        try {
            RoomScreenController roomController = new RoomScreenController();
            Scene scene = new Scene(roomController.getScreen().getRoot());
            Stage stage = (Stage) startButton.getScene().getWindow();

            // Preserve current stage size (should be 1024x720 to match all screens)
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            if (currentWidth > 0 && currentHeight > 0) {
                stage.setWidth(currentWidth);
                stage.setHeight(currentHeight);
            } else {
                stage.setWidth(1024);
                stage.setHeight(720);
            }

            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to navigate to Room Screen: " + e.getMessage());
            showAlert("Failed to start game. Please try again.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleNotificationClick() {
        // Close leaderboard if open
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            closeLeaderboard();
        }

        // Toggle notification popup
        if (notificationsPopup != null && notificationsPopup.isVisible()) {
            // If already visible, close it
            closePopup();
        } else {
            // Close settings if open, then show notifications
            if (settingsPopup != null) {
                settingsPopup.setVisible(false);
                settingsPopup.setManaged(false);
            }
            notificationManager.refreshNotifications();
            showNotificationPopup();
        }
    }

    private void showNotificationPopup() {
        if (popupOverlay != null && notificationsPopup != null) {
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);
            notificationsPopup.setVisible(true);
            notificationsPopup.setManaged(true);
        }
    }

    @FXML
    private void handleSettingsClick() {
        // Close leaderboard if open
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            closeLeaderboard();
        }

        // Toggle settings popup
        if (settingsPopup != null && settingsPopup.isVisible()) {
            // If already visible, close it
            closePopup();
        } else {
            // Close notifications if open, then show settings
            if (notificationsPopup != null) {
                notificationsPopup.setVisible(false);
                notificationsPopup.setManaged(false);
            }
            showSettingsPopup();
        }
    }

    private void showSettingsPopup() {
        if (popupOverlay != null && settingsPopup != null) {
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);
            settingsPopup.setVisible(true);
            settingsPopup.setManaged(true);
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.setContentText("You will need to login again to play.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performLogout();
            }
        });
    }

    private void performLogout() {
        try {
            // Disconnect TCP
            TCPClient.getInstance().disconnect();

            // Clear token
            TokenManager.getInstance().clearToken();

            // Navigate to Auth screen
            AuthScreenController authController = new AuthScreenController();
            Scene scene = new Scene(authController.getScreen().getRoot());
            Stage stage = (Stage) startButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("[MainScreen] Logout error: " + e.getMessage());
            showAlert("Logout failed. Please try again.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void closePopup() {
        // Only close settings and notifications popups
        // Leaderboard has its own close method
        boolean shouldHideOverlay = true;

        // Hide all popups
        if (helpPopup != null) {
            helpPopup.setVisible(false);
        }
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
            settingsPopup.setManaged(false);
        }
        if (notificationsPopup != null) {
            notificationsPopup.setVisible(false);
            notificationsPopup.setManaged(false);
        }

        // Only hide overlay if leaderboard is not visible
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            shouldHideOverlay = false;
        }

        if (popupOverlay != null && shouldHideOverlay) {
            popupOverlay.setVisible(false);
            popupOverlay.setManaged(false);
        }
    }

    private void handleViewProfile(Long userId, String displayName) {
        try {
            ProfileScreenController profileController = new ProfileScreenController(userId);
            Scene profileScene = new Scene(profileController.getScreen().getRoot());
            Stage stage = (Stage) personalAvatar.getScene().getWindow();
            Scene currentScene = personalAvatar.getScene();

            // Preserve current stage size
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            if (currentWidth > 0 && currentHeight > 0) {
                stage.setWidth(currentWidth);
                stage.setHeight(currentHeight);
            } else {
                stage.setWidth(1024);
                stage.setHeight(720);
            }

            // Store current scene for later reference
            final Scene mainScene = currentScene;
            final boolean isCurrentUserProfile = currentUser != null && userId.equals(currentUser.id);

            // Listen for when we return to MainScreen (only if viewing own profile)
            if (isCurrentUserProfile) {
                // Use a wrapper class to hold the listener reference
                final class ListenerWrapper {
                    javafx.beans.value.ChangeListener<Scene> listener;
                }
                final ListenerWrapper wrapper = new ListenerWrapper();
                wrapper.listener = (obs, oldScene, newScene) -> {
                    if (newScene == mainScene && oldScene == profileScene) {
                        // Just returned from viewing own profile, refresh user info
                        refreshCurrentUserInfo();
                        // Remove listener to prevent memory leak
                        if (wrapper.listener != null) {
                            stage.sceneProperty().removeListener(wrapper.listener);
                        }
                    }
                };
                stage.sceneProperty().addListener(wrapper.listener);
            }

            stage.setScene(profileScene);
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to navigate to Profile: " + e.getMessage());
            e.printStackTrace();
            showAlert("Unable to open profile page", Alert.AlertType.ERROR);
        }
    }

    private void handleOpenChat(Long userId, String displayName) {
        // Switch to chat area by focusing input; future: open direct chat tab
        if (chatInput != null) {
            chatInput.requestFocus();
            chatInput.setPromptText("Chat with " + displayName);
        }
        showAlert("Open chat with: " + displayName, Alert.AlertType.INFORMATION);
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
