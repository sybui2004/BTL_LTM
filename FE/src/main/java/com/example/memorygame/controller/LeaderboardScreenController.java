package com.example.memorygame.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.memorygame.controller.main.MainScreenFriendItemBuilder;
import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.FriendListDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.FriendApi;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.view.LeaderboardScreen;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LeaderboardScreenController {
    private final LeaderboardScreen screen;
    private UserSummary currentUser;

    @FXML
    private ToggleButton tabFriends;
    @FXML
    private ToggleButton tabGlobal;
    @FXML
    private ToggleGroup leaderboardTabGroup;
    @FXML
    private VBox leaderboardContainer;
    @FXML
    private VBox friendsListContainer;
    @FXML
    private BorderPane friendsPanel;
    @FXML
    private Button togglePanelButton;
    @FXML
    private Label personalElo;
    @FXML
    private Button startButton;

    private MainScreenFriendItemBuilder friendItemBuilder;
    private boolean panelExpanded = true;

    public LeaderboardScreenController() {
        try {
            this.screen = new LeaderboardScreen(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load LeaderboardScreen", e);
        }
    }

    public LeaderboardScreen getScreen() {
        return screen;
    }

    @FXML
    private void initialize() {
        loadCurrentUser();
        setupTabs();
        setupFriendItemBuilder();
        setupTCPListeners();
        loadLeaderboard(true); // Load friends leaderboard by default
        loadFriendsList();
        updateElo();
    }

    private void setupTCPListeners() {
        TCPClient client = TCPClient.getInstance();

        // Listen for friend status changes (accept/reject/remove) - refresh leaderboard
        client.onMessage("FRIEND_STATUS_CHANGED", message -> {
            System.out.println("[Leaderboard] Received friend status changed notification via TCP");
            // Add delay and retry to ensure backend API has updated
            refreshLeaderboardWithRetry(3, 500); // Retry 3 times with 500ms delay
        });
    }

    /**
     * Refresh leaderboard with delay to ensure backend API has updated
     */
    private void refreshLeaderboardWithRetry(int retries, long delayMs) {
        new Thread(() -> {
            try {
                // Wait for backend to update (initial delay)
                Thread.sleep(500); // Increased delay to ensure API has updated

                // Refresh current leaderboard tab and friends list
                Platform.runLater(() -> {
                    if (tabFriends != null && tabFriends.isSelected()) {
                        loadLeaderboard(true); // Refresh friends leaderboard
                    } else if (tabGlobal != null && tabGlobal.isSelected()) {
                        loadLeaderboard(false); // Refresh global leaderboard
                    }
                    loadFriendsList(); // Refresh friends side panel
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void loadCurrentUser() {
        new Thread(() -> {
            try {
                currentUser = UserApi.getCurrentUser();
            } catch (Exception e) {
                System.err.println("[LeaderboardScreen] Failed to load current user: " + e.getMessage());
            }
        }).start();
    }

    private void setupTabs() {
        if (leaderboardTabGroup == null || tabFriends == null || tabGlobal == null)
            return;

        tabFriends.setToggleGroup(leaderboardTabGroup);
        tabGlobal.setToggleGroup(leaderboardTabGroup);
        tabFriends.setSelected(true);

        leaderboardTabGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // Play button sound when user changes tab
            if (newT != null && oldT != newT) {
                SoundManager.playSound("button.wav");
            }
            if (newT == null)
                return;
            if (newT == tabFriends) {
                loadLeaderboard(true); // Friends
            } else {
                loadLeaderboard(false); // Global
            }
        });
    }

    private void setupFriendItemBuilder() {
        friendItemBuilder = new MainScreenFriendItemBuilder(
                this::loadUserAvatarOrFallback,
                this::handleViewProfile,
                null, // onSendFriendRequest - not needed here
                this::handleOpenChat);
    }

    private void loadLeaderboard(boolean friendsOnly) {
        new Thread(() -> {
            try {
                List<Map<String, Object>> rankings;

                if (friendsOnly) {
                    // Get friend ranking - filter global ranking by friends
                    rankings = getFriendRanking();
                } else {
                    // Get global ranking
                    rankings = UserApi.getRanking();
                }

                Platform.runLater(() -> displayLeaderboard(rankings));
            } catch (Exception e) {
                System.err.println("[LeaderboardScreen] Failed to load leaderboard: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private List<Map<String, Object>> getFriendRanking() {
        try {
            // Get all friends - refresh from API to ensure latest data
            FriendListDTO friendList = FriendApi.getFriendList();
            if (friendList == null || friendList.friends == null || friendList.friends.isEmpty()) {
                // Return empty list if no friends
                return Collections.emptyList();
            }

            // Get global ranking
            List<Map<String, Object>> globalRanking = UserApi.getRanking();
            if (globalRanking == null || globalRanking.isEmpty()) {
                return Collections.emptyList();
            }

            // Create set of friend IDs for quick lookup
            Set<Long> friendIds = new HashSet<>();
            for (FriendDTO friend : friendList.friends) {
                if (friend.id != null) {
                    friendIds.add(friend.id);
                }
            }

            // Add current user to set (current user should always appear in friends
            // leaderboard)
            if (currentUser != null && currentUser.id != 0) {
                friendIds.add(currentUser.id);
            }

            // Filter ranking to only include friends
            List<Map<String, Object>> friendRanking = new ArrayList<>();
            for (Map<String, Object> entry : globalRanking) {
                Object idObj = entry.get("id");
                if (idObj != null) {
                    Long id = ((Number) idObj).longValue();
                    if (friendIds.contains(id)) {
                        friendRanking.add(entry);
                    }
                }
            }

            // Sort by totalScore (Elo) descending
            friendRanking.sort((a, b) -> {
                Object scoreA = a.get("totalScore");
                Object scoreB = b.get("totalScore");
                int eloA = scoreA != null ? ((Number) scoreA).intValue() : 0;
                int eloB = scoreB != null ? ((Number) scoreB).intValue() : 0;
                return Integer.compare(eloB, eloA);
            });

            return friendRanking;
        } catch (Exception e) {
            System.err.println("[LeaderboardScreen] Failed to get friend ranking: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Public method to refresh leaderboard (can be called from outside)
     */
    public void refreshLeaderboard() {
        if (tabFriends != null && tabFriends.isSelected()) {
            loadLeaderboard(true);
        } else if (tabGlobal != null && tabGlobal.isSelected()) {
            loadLeaderboard(false);
        }
        loadFriendsList();
    }

    private void displayLeaderboard(List<Map<String, Object>> rankings) {
        if (leaderboardContainer == null)
            return;

        leaderboardContainer.getChildren().clear();

        int rank = 1;
        for (Map<String, Object> player : rankings) {
            HBox item = createLeaderboardItem(rank++, player);
            leaderboardContainer.getChildren().add(item);
        }
    }

    private HBox createLeaderboardItem(int rank, Map<String, Object> player) {
        // Layout cố định với các cột rõ ràng: Rank | Avatar | Name | Spacer | Elo |
        // Spacer | Win
        HBox item = new HBox(0); // Spacing = 0 để các phần tử sát nhau
        item.getStyleClass().add("leaderboard-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 15, 12, 15)); // Match với header padding

        // === CỘT 1: RANK (50px) ===
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("rank-number");
        rankLabel.setPrefWidth(50);
        rankLabel.setMinWidth(50);
        rankLabel.setMaxWidth(50);
        rankLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(rankLabel, Priority.NEVER);
        // Màu sắc theo rank
        if (rank == 1) {
            rankLabel.getStyleClass().add("rank-1");
        } else if (rank == 2) {
            rankLabel.getStyleClass().add("rank-2");
        } else if (rank == 3) {
            rankLabel.getStyleClass().add("rank-3");
        } else {
            rankLabel.getStyleClass().add("rank-4-plus");
        }

        // === CỘT 2: AVATAR (45px) ===
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefWidth(45);
        avatarContainer.setMinWidth(45);
        avatarContainer.setMaxWidth(45);
        avatarContainer.setAlignment(Pos.CENTER);
        HBox.setHgrow(avatarContainer, Priority.NEVER);

        // Load avatar từ API data
        String avatarUrl = player.get("avatarUrl") != null ? player.get("avatarUrl").toString() : null;
        ImageView avatar = new ImageView(loadUserAvatarOrFallback(avatarUrl));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);
        avatarContainer.getChildren().add(avatar);

        // === CỘT 3: NAME (250px) - Điền tên từ API ===
        Object userIdObj = player.get("id");
        Long userId = null;
        if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        } else if (userIdObj != null) {
            try {
                userId = Long.parseLong(userIdObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        String displayName = player.get("displayName") != null
                ? player.get("displayName").toString()
                : (player.get("username") != null ? player.get("username").toString() : "Player");
        final Long finalUserId = userId;
        final String finalDisplayName = displayName;

        boolean isCurrentUser = false;
        if (currentUser != null && userId != null) {
            isCurrentUser = userId.equals(currentUser.id);
        }

        Label nameLabel = new Label();
        if (isCurrentUser) {
            nameLabel.setText("It's Me");
            nameLabel.getStyleClass().addAll("player-name", "its-me");
        } else {
            nameLabel.setText(displayName); // Điền tên từ API
            nameLabel.getStyleClass().add("player-name");
        }
        // Đảm bảo width chính xác để thẳng hàng với header "Name"
        nameLabel.setPrefWidth(250);
        nameLabel.setMinWidth(250);
        nameLabel.setMaxWidth(250);
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        nameLabel.setWrapText(false);
        nameLabel.setPadding(new Insets(0)); // Không có padding
        HBox.setHgrow(nameLabel, Priority.NEVER);
        HBox.setMargin(nameLabel, Insets.EMPTY); // Không có margin

        // Click handler để mở profile
        if (finalUserId != null) {
            nameLabel.setCursor(javafx.scene.Cursor.HAND);
            nameLabel.setOnMouseClicked(e -> handleViewProfile(finalUserId, finalDisplayName));
            avatarContainer.setCursor(javafx.scene.Cursor.HAND);
            avatarContainer.setOnMouseClicked(e -> handleViewProfile(finalUserId, finalDisplayName));
        }

        // === SPACER (50px) - Khoảng cách giữa Name và Elo ===
        Region spacer1 = new Region();
        spacer1.setPrefWidth(50);
        spacer1.setMinWidth(50);
        spacer1.setMaxWidth(50);
        HBox.setHgrow(spacer1, Priority.NEVER);

        // === CỘT 4: ELO (100px) - Điền Elo từ API ===
        Object totalScoreObj = player.get("totalScore");
        int elo = totalScoreObj != null ? ((Number) totalScoreObj).intValue() : 0;
        Label eloLabel = new Label(String.valueOf(elo)); // Điền Elo từ API
        eloLabel.getStyleClass().add("player-elo");
        // Đảm bảo width chính xác để thẳng hàng với header "Elo"
        eloLabel.setPrefWidth(100);
        eloLabel.setMinWidth(100);
        eloLabel.setMaxWidth(100);
        eloLabel.setAlignment(Pos.CENTER_RIGHT);
        eloLabel.setWrapText(false);
        eloLabel.setPadding(new Insets(0)); // Không có padding
        HBox.setHgrow(eloLabel, Priority.NEVER);
        HBox.setMargin(eloLabel, Insets.EMPTY); // Không có margin

        // === SPACER (20px) - Khoảng cách giữa Elo và Win ===
        Region spacer2 = new Region();
        spacer2.setPrefWidth(20);
        spacer2.setMinWidth(20);
        spacer2.setMaxWidth(20);
        HBox.setHgrow(spacer2, Priority.NEVER);

        // === CỘT 5: WIN (100px) - Điền Win từ API ===
        Object winsObj = player.get("wins");
        int wins = winsObj != null ? ((Number) winsObj).intValue() : 0;
        Label winsLabel = new Label(String.valueOf(wins)); // Điền Win từ API
        winsLabel.getStyleClass().add("player-wins");
        // Đảm bảo width chính xác để thẳng hàng với header "Win"
        winsLabel.setPrefWidth(100);
        winsLabel.setMinWidth(100);
        winsLabel.setMaxWidth(100);
        winsLabel.setAlignment(Pos.CENTER_RIGHT);
        winsLabel.setWrapText(false);
        winsLabel.setPadding(new Insets(0)); // Không có padding
        HBox.setHgrow(winsLabel, Priority.NEVER);
        HBox.setMargin(winsLabel, Insets.EMPTY); // Không có margin

        // Thêm tất cả các phần tử vào HBox theo thứ tự cố định
        item.getChildren().addAll(rankLabel, avatarContainer, nameLabel, spacer1, eloLabel, spacer2, winsLabel);
        return item;
    }

    private void loadFriendsList() {
        new Thread(() -> {
            try {
                FriendListDTO friendList = FriendApi.getFriendList();
                if (friendList == null || friendList.friends == null) {
                    Platform.runLater(() -> {
                        if (friendsListContainer != null) {
                            friendsListContainer.getChildren().clear();
                        }
                    });
                    return;
                }

                Platform.runLater(() -> {
                    if (friendsListContainer == null)
                        return;
                    friendsListContainer.getChildren().clear();

                    for (FriendDTO friend : friendList.friends) {
                        HBox friendItem = friendItemBuilder.createFriendItem(friend);
                        friendsListContainer.getChildren().add(friendItem);
                    }
                });
            } catch (Exception e) {
                System.err.println("[LeaderboardScreen] Failed to load friends list: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleTogglePanel() {
        if (friendsPanel == null)
            return;

        SoundManager.playSound("button.wav");

        panelExpanded = !panelExpanded;

        TranslateTransition transition = new TranslateTransition(Duration.millis(300), friendsPanel);

        if (panelExpanded) {
            // Expand: move to position 0
            transition.setToX(0);
            friendsPanel.setVisible(true);
            friendsPanel.setManaged(true);
        } else {
            // Collapse: move right
            transition.setToX(friendsPanel.getWidth());
            transition.setOnFinished(e -> {
                friendsPanel.setVisible(false);
                friendsPanel.setManaged(false);
            });
        }

        transition.play();

        // Update button icon (rotate)
        if (togglePanelButton != null) {
            if (panelExpanded) {
                togglePanelButton.getGraphic().setRotate(0);
            } else {
                togglePanelButton.getGraphic().setRotate(180);
            }
        }
    }

    @FXML
    private void handleHelpClick() {
        SoundManager.playSound("button.wav");
        // TODO: Show help popup
    }

    @FXML
    private void handleSettingsClick() {
        SoundManager.playSound("button.wav");
        // TODO: Show settings popup
    }

    @FXML
    private void handleStartGame() {
        try {
            SoundManager.playSound("button.wav");
            RoomScreenController roomController = new RoomScreenController();
            Scene scene = new Scene(roomController.getScreen().getRoot());
            Stage stage = (Stage) startButton.getScene().getWindow();

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

            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("[LeaderboardScreen] Failed to navigate to Room Screen: " + e.getMessage());
        }
    }

    private void updateElo() {
        if (personalElo == null || currentUser == null)
            return;

        new Thread(() -> {
            try {
                Integer elo = UserApi.getUserElo(currentUser.id);
                Platform.runLater(() -> {
                    if (personalElo != null) {
                        personalElo.setText("Elo: " + (elo != null ? elo : 0));
                    }
                });
            } catch (Exception e) {
                System.err.println("[LeaderboardScreen] Failed to get Elo: " + e.getMessage());
            }
        }).start();
    }

    private Image loadUserAvatarOrFallback(String avatarUrl) {
        try {
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                String trimmed = avatarUrl.trim();
                String lower = trimmed.toLowerCase();
                // Check if it's a resource path (starts with /)
                if (lower.startsWith("/")) {
                    var resourceUrl = getClass().getResource(trimmed);
                    if (resourceUrl != null) {
                        return new Image(resourceUrl.toExternalForm(), 40, 40, true, true);
                    }
                } else if (lower.startsWith("http://") || lower.startsWith("https://")) {
                    return new Image(trimmed, 40, 40, true, true);
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        var url = getClass().getResource("/com/example/memorygame/assets/images/default-avatar.png");
        if (url == null) {
            // Try name.png as fallback
            url = getClass().getResource("/com/example/memorygame/assets/images/name.png");
        }
        if (url == null) {
            // Try alternative path in main directory
            url = getClass().getResource("/com/example/memorygame/name.png");
        }
        return new Image(Objects.requireNonNull(url).toExternalForm(), 40, 40, true, true);
    }

    private void handleViewProfile(Long userId, String displayName) {
        try {
            ProfileScreenController profileController = new ProfileScreenController(userId);
            Scene scene = new Scene(profileController.getScreen().getRoot());
            Stage stage = (Stage) leaderboardContainer.getScene().getWindow();

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
            System.err.println("[LeaderboardScreen] Failed to open profile: " + e.getMessage());
        }
    }

    private void handleOpenChat(Long userId, String displayName) {
        // TODO: Open chat with user
        System.out.println("Open chat with " + displayName);
    }
}
