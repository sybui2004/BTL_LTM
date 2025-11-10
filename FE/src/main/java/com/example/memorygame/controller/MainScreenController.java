package com.example.memorygame.controller;

import com.example.memorygame.controller.chat.PrivateChatController;
import com.example.memorygame.controller.main.FriendListUIManager;
import com.example.memorygame.controller.main.LeaderboardManager;
import com.example.memorygame.controller.main.NotificationManager;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.model.user.UserSettingDTO;
import com.example.memorygame.model.game.LeaderboardRow;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.model.chat.ChatError;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.MessageStatus;
import com.example.memorygame.view.MainScreen;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

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
    private Label chatInputLabel;
    @FXML
    private HBox bottomChatContainer;

    // Popups
    @FXML
    private StackPane popupOverlay;
    @FXML
    private VBox helpPopup;
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
    
    // Private Chat Popup
    @FXML
    private HBox privateChatPopupContainer;
    @FXML
    private BorderPane privateChatPopup;
    
    private PrivateChatController privateChatController;
    
    // World Chat Popup
    @FXML
    private StackPane worldChatPopupContainer;
    @FXML
    private VBox worldChatPopup;
    
    private com.example.memorygame.controller.chat.WorldChatController worldChatController;

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

    // Settings Popup (included component)
    // Note: JavaFX creates key "settingsPopupComponentController" from fx:id="settingsPopupComponent"
    @FXML
    private SettingsPopupController settingsPopupComponentController;
    
    // Convenience getter
    private SettingsPopupController getSettingsPopupController() {
        return settingsPopupComponentController;
    }

    // Notification badge
    @FXML
    private Circle notificationBadge;
    
    // Private chat badge
    @FXML
    private Circle privateChatBadge;

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
    
    // Store WorldChat UI root for reuse
    private javafx.scene.Parent worldChatRoot;

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
        // Đảm bảo popupOverlay không chặn events khi không visible
        if (popupOverlay != null) {
            popupOverlay.setMouseTransparent(true);
        }
        
        loadCurrentUser();
        setupGameBanner();
        setupStartButton();
        setupLeaderboardRankIcon();
        setupLeaderboardTable();
        setupSettingsPopup();
        initializeHelpers();
        loadData();
        setupTCPListeners();
        
        // Start background music after UI is loaded to avoid module access issues during FXML loading
        Platform.runLater(() -> {
            SoundManager.playBackgroundMusic("game_music_loop.wav");
        });
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
                
                // Đảm bảo StackPane có thể nhận mouse events
                startHexagonWrapper.setMouseTransparent(false);
                startHexagonWrapper.setPickOnBounds(true);
            });
        }

        // Tạo embossed effect cho START button text
        if (startButton != null) {
            // Đảm bảo button có thể nhận events
            startButton.setDisable(false);
            startButton.setMouseTransparent(false);
            
            // Thêm explicit event handler
            startButton.setOnAction(e -> handleStartGame());
            
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
        } else {
            System.err.println("[ERROR] startButton is null in setupStartButton!");
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
                System.out.println("[MainScreen] Loading avatar for user: " + currentUser.username + ", avatarUrl: " + currentUser.avatarUrl);
                Image avatar = loadUserAvatarOrFallback(currentUser.avatarUrl);
                
                // Set up error handler for failed loads
                avatar.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        System.err.println("[MainScreen] Failed to load avatar from: " + currentUser.avatarUrl);
                        // Try fallback
                        Image fallbackAvatar = loadUserAvatarOrFallback(null);
                        if (personalAvatar != null) {
                            personalAvatar.setImage(fallbackAvatar);
                        }
                    }
                });
                
                // Set image when loaded
                avatar.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() >= 1.0 && personalAvatar != null) {
                        personalAvatar.setImage(avatar);
                    }
                });
                
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
            
            setWorldChatCurrentUser();
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

        // Khởi tạo WorldChat controller sớm để bắt đầu lắng nghe tin nhắn
        initializeWorldChatController();

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

    /**
     * Khởi tạo WorldChat controller sớm để bắt đầu lắng nghe tin nhắn
     * Cho phép nhận tin nhắn World Chat ngay cả khi popup chưa được mở
     */
    private void initializeWorldChatController() {
        if (worldChatController != null) {
            return;
        }
        
        try {
            // Load WorldChat FXML và lưu UI root để tái sử dụng
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/chat/WorldChat.fxml"));
            worldChatRoot = loader.load();
            worldChatController = loader.getController();
            
            // Set user nếu đã có sẵn, nếu không sẽ được set sau trong setWorldChatCurrentUser()
            if (currentUser != null) {
                worldChatController.setCurrentUser(currentUser);
            }
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to initialize WorldChat controller: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Set current user cho WorldChat controller sau khi user được load
     */
    private void setWorldChatCurrentUser() {
        if (worldChatController != null && currentUser != null) {
            worldChatController.setCurrentUser(currentUser);
        }
    }

    private void loadData() {
        leaderboardManager.loadTop3();
        notificationManager.loadFriendRequests();
        friendListManager.loadFriends();
    }

    private void setupTCPListeners() {
        TCPClient client = TCPClient.getInstance();

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
        
        // Lắng nghe tin nhắn private chat để hiển thị badge
        client.addChatListener(new TCPClient.ChatMessageListener() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                if (message.getType() == com.example.memorygame.model.chat.ChatType.PRIVATE) {
                    handleIncomingPrivateChatForBadge(message);
                }
            }
            
            @Override
            public void onMessageStatusChanged(String messageId, MessageStatus status) {
            }
            
            @Override
            public void onError(ChatError error) {
            }
        });
    }
    
    /**
     * Xử lý tin nhắn private chat đến để hiển thị notification badge
     */
    private void handleIncomingPrivateChatForBadge(ChatMessage message) {
        try {
            if (message == null || message.getSender() == null) {
                return;
            }
            
            long senderId = message.getSender().id;
            long receiverId = -1;
            
            if (message.getReceiver() != null) {
                receiverId = message.getReceiver().id;
            } else {
                receiverId = currentUser != null ? currentUser.id : -1;
            }
            
            // Hiển thị badge nếu tin nhắn dành cho user hiện tại và không phải do user này gửi
            boolean shouldShowBadge = currentUser != null && receiverId == currentUser.id && senderId != currentUser.id && privateChatBadge != null;
            
            if (shouldShowBadge) {
                Platform.runLater(() -> {
                    privateChatBadge.setVisible(true);
                    SoundManager.playSound("mess_noti.mp3");
                });
            }
            
        } catch (Exception e) {
            System.err.println("[MainScreen] Lỗi xử lý tin nhắn private chat: " + e.getMessage());
        }
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
        SoundManager.playSound("button.wav");
        // Close any other popups first
        closeAllPopups();
        
        // Close private chat if open
        if (privateChatPopupContainer != null && privateChatPopupContainer.isVisible()) {
            closePrivateChat();
        }

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
        popupOverlay.setMouseTransparent(false); // Cho phép nhận mouse events khi hiển thị

        // Animate slide in from left (leaderboard from left edge)
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), leaderboardPopupContainer);
        slideIn.setToX(0); // Slide to position 0 (left edge of screen)
        slideIn.play();
    }

    private void closeAllPopups() {
        SettingsPopupController settingsController = getSettingsPopupController();
        if (settingsController != null) {
            settingsController.hide();
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
                popupOverlay.setMouseTransparent(true); // Không chặn events khi ẩn
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

    /**
     * Toggle WorldChat popup khi click vào chat input ở bottom bar
     */
    @FXML
    private void handleChatInputClick() {
        if (worldChatPopupContainer != null && worldChatPopupContainer.isVisible()) {
            closeWorldChat();
        } else {
            showWorldChatPopup();
        }
    }

    @FXML
    private void handleOverlayClick(javafx.scene.input.MouseEvent event) {
        SettingsPopupController controller = getSettingsPopupController();
        if (controller != null) {
            VBox settingsPopup = controller.getSettingsPopup();
            if (settingsPopup != null && settingsPopup.isVisible()) {
                controller.hide();
            }
        }
    }

    @FXML
    private void handleStartGame() {
        SoundManager.playSound("button.wav");
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
            e.printStackTrace();
            showAlert("Failed to start game. Please try again.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleNotificationClick() {
        SoundManager.playSound("button.wav");
        // Close leaderboard if open
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            closeLeaderboard();
        }
        
        // Close private chat if open
        if (privateChatPopupContainer != null && privateChatPopupContainer.isVisible()) {
            closePrivateChat();
        }

        // Toggle notification popup
        if (notificationsPopup != null && notificationsPopup.isVisible()) {
            // If already visible, close it
            closePopup();
        } else {
            SettingsPopupController settingsController = getSettingsPopupController();
            if (settingsController != null) {
                settingsController.hide();
            }
            notificationManager.refreshNotifications();
            showNotificationPopup();
        }
    }

    private void showNotificationPopup() {
        if (popupOverlay != null && notificationsPopup != null) {
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);
            popupOverlay.setMouseTransparent(false); // Cho phép nhận mouse events khi hiển thị
            notificationsPopup.setVisible(true);
            notificationsPopup.setManaged(true);
        }
    }

    private void setupSettingsPopup() {
        SettingsPopupController controller = getSettingsPopupController();
        if (controller != null) {
            controller.setCurrentUser(currentUser);
            // Set parent overlay reference
            controller.setParentOverlay(popupOverlay);
            // Load settings on app start to apply notification enabled state
            loadSettingsOnStart();
            System.out.println("[MainScreen] SettingsPopupController setup completed");
        } else {
            System.err.println("[MainScreen] SettingsPopupController is null in setupSettingsPopup!");
        }
    }
    
    /**
     * Load user settings on app start to apply notification enabled state
     */
    private void loadSettingsOnStart() {
        if (currentUser == null) {
            return;
        }
        new Thread(() -> {
            try {
                UserSettingDTO settings = UserApi.getSettings(currentUser.id);
                if (settings != null) {
                    Platform.runLater(() -> {
                        // Apply notification enabled state to SoundManager
                        SoundManager.setNotificationEnabled(settings.notification);
                        // Also apply volume settings
                        SoundManager.setBackgroundMusicVolume(settings.musicVolume / 100.0);
                        SoundManager.setSoundFxVolume(settings.soundFxVolume / 100.0);
                    });
                }
            } catch (Exception e) {
                System.err.println("[MainScreen] Failed to load settings on start: " + e.getMessage());
            }
        }).start();
    }
    
    @FXML
    private void handleSettingsClick() {
        SoundManager.playSound("button.wav");
        // Close leaderboard if open
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            closeLeaderboard();
        }
        
        // Close private chat if open
        if (privateChatPopupContainer != null && privateChatPopupContainer.isVisible()) {
            closePrivateChat();
        }

        // Toggle settings popup
        SettingsPopupController controller = getSettingsPopupController();
        if (controller == null) {
            System.err.println("[MainScreen] SettingsPopupController is null! Cannot show settings.");
            return;
        }
        
        VBox settingsPopup = controller.getSettingsPopup();
        if (settingsPopup == null) {
            System.err.println("[MainScreen] SettingsPopup VBox is null!");
            return;
        }
        
        if (settingsPopup.isVisible()) {
            // If already visible, close it
            controller.hide();
        } else {
            // Close notifications if open, then show settings
            if (notificationsPopup != null) {
                notificationsPopup.setVisible(false);
                notificationsPopup.setManaged(false);
            }
            controller.show();
        }
    }


    @FXML
    private void closePopup() {
        boolean shouldHideOverlay = true;

        if (helpPopup != null) {
            helpPopup.setVisible(false);
        }
        
        SettingsPopupController settingsController = getSettingsPopupController();
        if (settingsController != null) {
            VBox settingsPopup = settingsController.getSettingsPopup();
            if (settingsPopup != null && settingsPopup.isVisible()) {
                settingsController.hide();
            }
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
            popupOverlay.setMouseTransparent(true); // Không chặn events khi ẩn
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
        if (userId == null) {
            showAlert("Invalid user", Alert.AlertType.ERROR);
            return;
        }
        
        UserSummary otherUser = new UserSummary();
        otherUser.id = userId;
        otherUser.displayName = displayName;
        
        showPrivateChatPopupWithUser(otherUser);
    }

    @FXML
    private void handlePrivateChatClick() {
        SoundManager.playSound("button.wav");
        closeAllPopups();
        
        if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
            closeLeaderboard();
        }
        
        if (privateChatBadge != null) {
            privateChatBadge.setVisible(false);
        }
        
        showPrivateChatPopup();
    }
    
    private void showPrivateChatPopup() {
        showPrivateChatPopupWithUser(null);
    }
    
    private void showPrivateChatPopupWithUser(UserSummary targetUser) {
        if (privateChatPopupContainer == null || popupOverlay == null || privateChatPopup == null)
            return;
        
        try {
            boolean wasAlreadyLoaded = (privateChatController != null);
            
            if (privateChatController == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/chat/PrivateChat.fxml"));
                javafx.scene.Parent privateChatRoot = loader.load();
                privateChatController = loader.getController();
                
                if (currentUser != null) {
                    privateChatController.setCurrentUser(currentUser);
                }
                
                privateChatPopup.getChildren().clear();
                privateChatPopup.setCenter(privateChatRoot);
            } else {
                if (currentUser != null) {
                    privateChatController.setCurrentUser(currentUser);
                }
            }
            
            privateChatPopupContainer.setTranslateX(680);
            privateChatPopupContainer.setVisible(true);
            privateChatPopupContainer.setManaged(true);
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);
            popupOverlay.setMouseTransparent(false);
            
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), privateChatPopupContainer);
            slideIn.setToX(0);
            
            if (wasAlreadyLoaded && targetUser != null && privateChatController != null) {
                Platform.runLater(() -> {
                    privateChatController.startConversation(targetUser);
                });
            } else {
                slideIn.setOnFinished(e -> {
                    if (targetUser != null && privateChatController != null) {
                        Platform.runLater(() -> {
                            privateChatController.startConversation(targetUser);
                        });
                    }
                });
            }
            slideIn.play();
        } catch (Exception e) {
            System.err.println("[MainScreen] Không thể tải Private Chat: " + e.getMessage());
            e.printStackTrace();
            showAlert("Failed to open private chat. Please try again.", Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void closePrivateChat() {
        if (privateChatPopupContainer == null)
            return;
        
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), privateChatPopupContainer);
        slideOut.setToX(680);
        slideOut.setOnFinished(e -> {
            privateChatPopupContainer.setVisible(false);
            privateChatPopupContainer.setManaged(false);
            if (popupOverlay != null) {
                popupOverlay.setVisible(false);
                popupOverlay.setManaged(false);
                popupOverlay.setMouseTransparent(true);
            }
        });
        slideOut.play();
    }
    
    /**
     * Hiển thị WorldChat popup, căn chỉnh với chat input ở bottom bar
     * Popup sẽ xuất hiện phía trên chat input
     */
    private void showWorldChatPopup() {
        if (worldChatPopupContainer == null || worldChatPopup == null || bottomChatContainer == null)
            return;
        
        try {
            // Đóng các popup khác trước
            closeAllPopups();
            if (leaderboardPopupContainer != null && leaderboardPopupContainer.isVisible()) {
                closeLeaderboard();
            }
            if (privateChatPopupContainer != null && privateChatPopupContainer.isVisible()) {
                closePrivateChat();
            }
            
            // Thêm WorldChat UI vào popup nếu chưa có (controller đã được khởi tạo sớm)
            if (worldChatPopup.getChildren().isEmpty()) {
                if (worldChatController == null || worldChatRoot == null) {
                    initializeWorldChatController();
                }
                
                if (worldChatRoot != null) {
                    worldChatPopup.getChildren().clear();
                    worldChatPopup.getChildren().add(worldChatRoot);
                }
            }
            
            // Tính toán vị trí popup dựa trên vị trí chat input
            worldChatPopupContainer.setTranslateX(0);
            worldChatPopupContainer.setTranslateY(0);
            worldChatPopupContainer.setOpacity(0);
            worldChatPopupContainer.setVisible(true);
            worldChatPopupContainer.setManaged(true);
            worldChatPopupContainer.layout();
            
            Platform.runLater(() -> {
                // Lấy vị trí chat input trong scene
                Bounds chatInputBounds = bottomChatContainer.localToScene(bottomChatContainer.getBoundsInLocal());
                double chatInputX = chatInputBounds.getMinX();
                double chatInputY = chatInputBounds.getMinY();
                
                // Lấy vị trí container hiện tại
                Bounds containerBounds = worldChatPopupContainer.localToScene(worldChatPopupContainer.getBoundsInLocal());
                double containerX = containerBounds.getMinX();
                double containerY = containerBounds.getMinY();
                
                // Đặt popup phía trên chat input, căn trái
                double targetX = chatInputX - 15;
                double targetY = chatInputY - 500 - 285 + 50;
                
                // Đảm bảo popup không vượt quá màn hình
                if (targetY < 10) {
                    targetY = 10;
                }
                
                // Tính toán translation cần thiết
                double translateX = targetX - containerX;
                double translateY = targetY - containerY;
                
                worldChatPopupContainer.setTranslateX(translateX);
                worldChatPopupContainer.setTranslateY(translateY);
                
                // Fade in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), worldChatPopupContainer);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            
        } catch (Exception e) {
            System.err.println("[MainScreen] Failed to load World Chat: " + e.getMessage());
            e.printStackTrace();
            showAlert("Failed to open world chat. Please try again.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Đóng WorldChat popup với fade out animation
     */
    @FXML
    private void closeWorldChat() {
        if (worldChatPopupContainer == null || !worldChatPopupContainer.isVisible())
            return;
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), worldChatPopupContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            worldChatPopupContainer.setVisible(false);
            worldChatPopupContainer.setManaged(false);
            worldChatPopupContainer.setTranslateX(0);
            worldChatPopupContainer.setTranslateY(0);
        });
        fadeOut.play();
    }
    
    /**
     * Xử lý click vào WorldChat popup - consume event để không đóng popup
     */
    @FXML
    private void handleWorldChatPopupClick(MouseEvent event) {
        event.consume();
    }
    
    /**
     * Xử lý click vào overlay - đóng popup nếu click bên ngoài
     */
    @FXML
    private void handleWorldChatOverlayClick(MouseEvent event) {
        if (worldChatPopupContainer == null || !worldChatPopupContainer.isVisible())
            return;
        
        if (worldChatPopup != null) {
            Bounds popupBounds = worldChatPopup.localToScene(worldChatPopup.getBoundsInLocal());
            double clickX = event.getSceneX();
            double clickY = event.getSceneY();
            
            // Đóng popup nếu click bên ngoài bounds
            if (clickX < popupBounds.getMinX() || clickX > popupBounds.getMaxX() ||
                clickY < popupBounds.getMinY() || clickY > popupBounds.getMaxY()) {
                closeWorldChat();
            }
        } else {
            closeWorldChat();
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Image loadUserAvatarOrFallback(String candidateUrl) {
        try {
            if (candidateUrl != null) {
                String trimmed = candidateUrl.trim();
                if (!trimmed.isEmpty()) {
                    String lower = trimmed.toLowerCase();
                    
                    // Xử lý đường dẫn từ DB: /static/avatars/default_avatar.png
                    if (lower.startsWith("/static/")) {
                        // Đây là server path, prepend base URL
                        String imageUrl = "http://localhost:8080" + trimmed;
                        System.out.println("[MainScreen] Loading avatar from server: " + imageUrl);
                        return new Image(imageUrl, true);
                    }
                    // Xử lý URL đầy đủ
                    else if (lower.startsWith("http://") || lower.startsWith("https://")) {
                        System.out.println("[MainScreen] Loading avatar from full URL: " + trimmed);
                        return new Image(trimmed, true);
                    }
                    // Xử lý đường dẫn tương đối (không bắt đầu bằng /)
                    else if (!trimmed.startsWith("/")) {
                        // Nếu không có / ở đầu, coi như là filename trong /static/avatars/
                        String imageUrl = "http://localhost:8080/static/avatars/" + trimmed;
                        System.out.println("[MainScreen] Loading avatar from relative path: " + imageUrl);
                        return new Image(imageUrl, true);
                    }
                    // Thử tìm trong resource path (classpath)
                    else {
                        var resourceUrl = getClass().getResource(trimmed);
                        if (resourceUrl != null) {
                            System.out.println("[MainScreen] Loading avatar from resource: " + resourceUrl.toExternalForm());
                            return new Image(resourceUrl.toExternalForm(), true);
                        } else {
                            System.out.println("[MainScreen] Resource not found: " + trimmed);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[MainScreen] IllegalArgumentException loading avatar: " + e.getMessage());
        }

        // Try multiple fallback paths
        String[] fallbackPaths = {
            "/com/example/memorygame/assets/images/name.png",
            "/com/example/memorygame/assets/images/avt1.png",
            "/com/example/memorygame/assets/images/avatar/avatar2.png",
            "/com/example/memorygame/name.png"
        };
        
        for (String fallbackPath : fallbackPaths) {
            var url = getClass().getResource(fallbackPath);
            if (url != null) {
                return new Image(url.toExternalForm(), true);
            }
        }
        
        // If all fallbacks fail, use server default avatar
        String serverDefaultAvatarUrl = "http://localhost:8080/static/avatars/default_avatar.png";
        System.err.println("[MainScreen] All fallback avatar resources not found, using server default: " + serverDefaultAvatarUrl);
        return new Image(serverDefaultAvatarUrl, true);
    }
}
