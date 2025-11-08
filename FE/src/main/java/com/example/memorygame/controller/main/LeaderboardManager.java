package com.example.memorygame.controller.main;

import com.example.memorygame.model.game.LeaderboardRow;
import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.FriendListDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.FriendApi;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.function.Function;

/**
 * Manages leaderboard display (top 3 and full leaderboard)
 */
public class LeaderboardManager {
    private final VBox top3Container;
    private final TableView<LeaderboardRow> leaderboardTable;
    private final Function<String, Image> avatarLoader;
    private java.util.function.BiConsumer<Long, String> onViewProfile;
    private UserSummary currentUser;
    private final ObservableList<LeaderboardRow> leaderboardData = FXCollections.observableArrayList();

    public LeaderboardManager(VBox top3Container, TableView<LeaderboardRow> leaderboardTable,
            Function<String, Image> avatarLoader) {
        this.top3Container = top3Container;
        this.leaderboardTable = leaderboardTable;
        this.avatarLoader = avatarLoader;

        if (leaderboardTable != null) {
            leaderboardTable.setItems(leaderboardData);
        }

        // Load current user
        new Thread(() -> {
            try {
                currentUser = UserApi.getCurrentUser();
            } catch (Exception e) {
                System.err.println("[LeaderboardManager] Failed to load current user: " + e.getMessage());
            }
        }).start();
    }

    public void setOnViewProfile(java.util.function.BiConsumer<Long, String> onViewProfile) {
        this.onViewProfile = onViewProfile;
    }

    public void loadLeaderboard(boolean friendsOnly) {
        new Thread(() -> {
            try {
                List<Map<String, Object>> rankings;

                if (friendsOnly) {
                    rankings = getFriendRanking();
                } else {
                    rankings = UserApi.getRanking();
                }

                Platform.runLater(() -> displayFullLeaderboard(rankings));
            } catch (Exception e) {
                System.err.println("[LeaderboardManager] Failed to load leaderboard: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private List<Map<String, Object>> getFriendRanking() {
        try {
            FriendListDTO friendList = FriendApi.getFriendList();
            if (friendList == null || friendList.friends == null || friendList.friends.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> globalRanking = UserApi.getRanking();
            if (globalRanking == null || globalRanking.isEmpty()) {
                return Collections.emptyList();
            }

            Set<Long> friendIds = new HashSet<>();
            for (FriendDTO friend : friendList.friends) {
                friendIds.add(friend.id);
            }

            // Add current user to set
            if (currentUser != null && currentUser.id != 0) {
                friendIds.add(currentUser.id);
            }

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

            friendRanking.sort((a, b) -> {
                Object scoreA = a.get("totalScore");
                Object scoreB = b.get("totalScore");
                int eloA = scoreA != null ? ((Number) scoreA).intValue() : 0;
                int eloB = scoreB != null ? ((Number) scoreB).intValue() : 0;
                return Integer.compare(eloB, eloA);
            });

            return friendRanking;
        } catch (Exception e) {
            System.err.println("[LeaderboardManager] Failed to get friend ranking: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void loadTop3() {
        new Thread(() -> {
            try {
                List<Map<String, Object>> rankings = UserApi.getRanking();
                Platform.runLater(() -> displayTop3(rankings));
            } catch (Exception e) {
                System.err.println("[Leaderboard] Failed to load top 3: " + e.getMessage());
            }
        }).start();
    }

    public void loadFullLeaderboard() {
        new Thread(() -> {
            try {
                List<Map<String, Object>> rankings = UserApi.getRanking();
                Platform.runLater(() -> displayFullLeaderboard(rankings));
            } catch (Exception e) {
                System.err.println("[Leaderboard] Failed to load full leaderboard: " + e.getMessage());
            }
        }).start();
    }

    private void displayTop3(List<Map<String, Object>> rankings) {
        if (top3Container == null)
            return;

        top3Container.getChildren().clear();

        int count = Math.min(3, rankings.size());
        for (int i = 0; i < count; i++) {
            Map<String, Object> player = rankings.get(i);
            top3Container.getChildren().add(createTop3Item(i + 1, player));
        }
    }

    private HBox createTop3Item(int rank, Map<String, Object> player) {
        // Chỉ hiển thị avatar, không có khung trắng - giống avatar đại diện
        HBox item = new HBox(0);
        item.setAlignment(Pos.CENTER);
        item.getStyleClass().add("leaderboard-item-top3");

        // Thêm class theo rank để có màu sắc khác nhau
        if (rank == 1) {
            item.getStyleClass().add("rank-1");
        } else if (rank == 2) {
            item.getStyleClass().add("rank-2");
        } else if (rank == 3) {
            item.getStyleClass().add("rank-3");
        }

        // Avatar hiển thị dạng vuông với bo góc nhẹ
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(78, 78); // Match với container size
        avatarContainer.setMinSize(78, 78);
        avatarContainer.setMaxSize(78, 78);
        avatarContainer.setCursor(javafx.scene.Cursor.HAND); // Show hand cursor
        String avatarUrl = player.get("avatarUrl") != null ? player.get("avatarUrl").toString() : null;
        ImageView avatar = new ImageView(avatarLoader.apply(avatarUrl));
        avatar.setFitWidth(78);
        avatar.setFitHeight(78);
        avatar.setPreserveRatio(false);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(78, 78);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        avatar.setClip(clip);

        // Get user ID and display name for profile view
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

        String displayName = player.get("displayName") != null ? player.get("displayName").toString()
                : (player.get("username") != null ? player.get("username").toString() : "Player");
        final Long finalUserId = userId;
        final String finalDisplayName = displayName;

        // Add click handler to open profile
        if (finalUserId != null) {
            avatarContainer.setOnMouseClicked(e -> {
                if (onViewProfile != null) {
                    Platform.runLater(() -> onViewProfile.accept(finalUserId, finalDisplayName));
                }
            });
        }

        // Add avatar first
        avatarContainer.getChildren().add(avatar);

        // Rank number badge
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("rank-number-small");
        StackPane.setAlignment(rankLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(rankLabel, new javafx.geometry.Insets(6, 6, 0, 0));
        avatarContainer.getChildren().add(rankLabel);

        // Chỉ hiển thị avatar với rank badge, không có tên (giống ảnh)
        item.getChildren().add(avatarContainer);
        return item;
    }

    private void displayFullLeaderboard(List<Map<String, Object>> rankings) {
        if (leaderboardTable == null)
            return;

        leaderboardData.clear();
        int rank = 1;
        for (Map<String, Object> player : rankings) {
            // Debug: Print player data to check API response
            System.out.println("[LeaderboardManager] Player data: " + player);

            // Extract data from player map
            String rankStr = String.valueOf(rank++);

            String displayName = player.get("displayName") != null
                    ? player.get("displayName").toString()
                    : (player.get("username") != null ? player.get("username").toString() : "Player");

            // Get Elo/totalScore
            Object totalScoreObj = player.get("totalScore");
            if (totalScoreObj == null) {
                totalScoreObj = player.get("total_score");
            }
            if (totalScoreObj == null) {
                totalScoreObj = player.get("elo");
            }
            int eloValue = 0;
            if (totalScoreObj != null) {
                if (totalScoreObj instanceof Number) {
                    eloValue = ((Number) totalScoreObj).intValue();
                } else {
                    try {
                        eloValue = Integer.parseInt(totalScoreObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            String eloStr = String.valueOf(eloValue);

            // Get Wins
            Object winsObj = player.get("winCount");
            if (winsObj == null) {
                winsObj = player.get("wins");
            }
            if (winsObj == null) {
                winsObj = player.get("win_count");
            }
            int winsValue = 0;
            if (winsObj != null) {
                if (winsObj instanceof Number) {
                    winsValue = ((Number) winsObj).intValue();
                } else {
                    try {
                        winsValue = Integer.parseInt(winsObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            String winsStr = String.valueOf(winsValue);

            // Get Avatar
            String avatarUrl = player.get("avatarUrl") != null ? player.get("avatarUrl").toString() : null;
            Image avatar = avatarLoader.apply(avatarUrl);

            // Get User ID
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

            leaderboardData.add(new LeaderboardRow(rankStr, displayName, eloStr, winsStr, avatar, userId));
        }
    }

    private HBox createFullLeaderboardItem(int rank, Map<String, Object> player) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);

        // Check if this is current user and apply appropriate style
        if (currentUser != null && currentUser.id != 0) {
            Object idObj = player.get("id");
            if (idObj != null) {
                Long playerId = ((Number) idObj).longValue();
                if (playerId == currentUser.id) {
                    item.getStyleClass().add("leaderboard-item-popup-current");
                } else {
                    item.getStyleClass().add("leaderboard-item-popup");
                }
            } else {
                item.getStyleClass().add("leaderboard-item-popup");
            }
        } else {
            item.getStyleClass().add("leaderboard-item-popup");
        }

        item.setPadding(new Insets(12));

        // Rank number
        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.getStyleClass().add("rank-number-popup");

        // Avatar
        StackPane avatarContainer = new StackPane();
        String avatarUrl = player.get("avatarUrl") != null ? player.get("avatarUrl").toString() : null;
        ImageView avatar = new ImageView(avatarLoader.apply(avatarUrl));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);
        avatarContainer.getChildren().add(avatar);

        // Name - left aligned, grows to fill space
        String displayName = player.get("displayName") != null
                ? player.get("displayName").toString()
                : (player.get("username") != null ? player.get("username").toString() : "Player");
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("player-name-popup");
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Elo - center aligned, fixed width
        Object totalScoreObj = player.get("totalScore");
        if (totalScoreObj == null) {
            totalScoreObj = player.get("total_score"); // Try snake_case
        }
        if (totalScoreObj == null) {
            totalScoreObj = player.get("elo"); // Try elo directly
        }
        int elo = 0;
        if (totalScoreObj != null) {
            if (totalScoreObj instanceof Number) {
                elo = ((Number) totalScoreObj).intValue();
            } else {
                try {
                    elo = Integer.parseInt(totalScoreObj.toString());
                } catch (NumberFormatException e) {
                    System.err.println("[LeaderboardManager] Failed to parse Elo: " + totalScoreObj);
                }
            }
        } else {
            System.err.println("[LeaderboardManager] Elo not found in player data. Keys: " + player.keySet());
        }
        Label eloLabel = new Label(String.valueOf(elo));
        eloLabel.getStyleClass().add("player-elo-popup");
        eloLabel.setAlignment(Pos.CENTER);
        eloLabel.setPrefWidth(100);
        eloLabel.setMinWidth(100);
        eloLabel.setMaxWidth(100);

        // Wins - center aligned, fixed width
        Object winsObj = player.get("winCount");
        if (winsObj == null) {
            winsObj = player.get("wins"); // Fallback for compatibility
        }
        if (winsObj == null) {
            winsObj = player.get("win_count"); // Try snake_case
        }
        int wins = 0;
        if (winsObj != null) {
            if (winsObj instanceof Number) {
                wins = ((Number) winsObj).intValue();
            } else {
                try {
                    wins = Integer.parseInt(winsObj.toString());
                } catch (NumberFormatException e) {
                    System.err.println("[LeaderboardManager] Failed to parse Wins: " + winsObj);
                }
            }
        }
        Label winsLabel = new Label(String.valueOf(wins));
        winsLabel.getStyleClass().add("player-wins-popup");
        winsLabel.setAlignment(Pos.CENTER);
        winsLabel.setPrefWidth(100);
        winsLabel.setMinWidth(100);
        winsLabel.setMaxWidth(100);

        item.getChildren().addAll(rankLabel, avatarContainer, nameLabel, eloLabel, winsLabel);
        return item;
    }
}
