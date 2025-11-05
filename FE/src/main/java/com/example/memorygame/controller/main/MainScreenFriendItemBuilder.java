package com.example.memorygame.controller.main;

import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Builds friend list items for MainScreen with profile view and add friend
 * features
 */
public class MainScreenFriendItemBuilder {
    private final Function<String, Image> imageLoader;
    private final BiConsumer<Long, String> onViewProfile; // userId, displayName
    private final BiConsumer<Long, String> onSendFriendRequest; // userId, displayName
    private final BiConsumer<Long, String> onOpenChat; // userId, displayName

    public MainScreenFriendItemBuilder(
            Function<String, Image> imageLoader,
            BiConsumer<Long, String> onViewProfile,
            BiConsumer<Long, String> onSendFriendRequest,
            BiConsumer<Long, String> onOpenChat) {
        this.imageLoader = imageLoader;
        this.onViewProfile = onViewProfile;
        this.onSendFriendRequest = onSendFriendRequest;
        this.onOpenChat = onOpenChat;
    }

    /**
     * Create friend item (for existing friends)
     */
    public HBox createFriendItem(FriendDTO friend) {
        HBox row = new HBox(12);
        row.getStyleClass().add("friend-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));

        // Avatar with click to view profile
        StackPane avatarContainer = new StackPane();
        avatarContainer.setCursor(Cursor.HAND);

        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        avatar.setImage(imageLoader.apply(friend.avatarUrl));

        // Rounded avatar
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        // Status indicator
        Circle statusIndicator = new Circle(6);
        statusIndicator.getStyleClass().add(getStatusClass(friend.status));
        StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(statusIndicator, new Insets(0, 0, 2, 0));

        avatarContainer.getChildren().addAll(avatar, statusIndicator);

        // Click avatar to view profile
        avatarContainer.setOnMouseClicked(e -> {
            String displayName = (friend.displayName != null && !friend.displayName.isBlank())
                    ? friend.displayName
                    : "Player";
            onViewProfile.accept(friend.id, displayName);
        });

        // Friend info
        VBox info = new VBox(3);
        String displayName = (friend.displayName != null && !friend.displayName.isBlank())
                ? friend.displayName
                : "Player";
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("friend-name");

        // Status và Elo tách riêng - chỉ status có màu
        String statusText = mapStatus(friend.status);
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(friend.status));
        
        Label eloLabel = new Label("Elo: ...");
        eloLabel.getStyleClass().add("friend-elo-text");
        
        // Load Elo từ API async
        if (friend.id != null) {
            new Thread(() -> {
                try {
                    Integer elo = UserApi.getUserElo(friend.id);
                    Platform.runLater(() -> {
                        eloLabel.setText("Elo: " + (elo != null ? elo : 0));
                    });
                } catch (Exception e) {
                    System.err.println("[FriendItem] Failed to load Elo for user " + friend.id + ": " + e.getMessage());
                    Platform.runLater(() -> {
                        eloLabel.setText("Elo: 0");
                    });
                }
            }).start();
        }

        // HBox để hiển thị status và Elo trên cùng 1 dòng
        HBox statusEloBox = new HBox(5);
        statusEloBox.setAlignment(Pos.CENTER_LEFT);
        statusEloBox.getChildren().addAll(statusLabel, eloLabel);

        info.getChildren().addAll(nameLabel, statusEloBox);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Chat icon (for existing friends)
        ImageView chatIcon = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResource("/com/example/memorygame/assets/images/icon/chat.png")).toExternalForm()));
        chatIcon.setFitWidth(24);
        chatIcon.setFitHeight(24);
        chatIcon.setPreserveRatio(true);

        StackPane chatBtn = new StackPane(chatIcon);
        chatBtn.getStyleClass().add("icon-button-main");
        chatBtn.setCursor(Cursor.HAND);
        chatBtn.setOnMouseClicked(e -> onOpenChat.accept(friend.id, displayName));

        row.getChildren().addAll(avatarContainer, info, chatBtn);
        return row;
    }

    /**
     * Create stranger item (for users who are not friends yet)
     * @param stranger The stranger user data
     * @param hasOutgoingRequest Whether there's already an outgoing friend request to this user
     */
    public HBox createStrangerItem(FriendDTO stranger, boolean hasOutgoingRequest) {
        HBox row = new HBox(12);
        row.getStyleClass().add("friend-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));

        // Avatar with click to view profile
        StackPane avatarContainer = new StackPane();
        avatarContainer.setCursor(Cursor.HAND);

        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        avatar.setImage(imageLoader.apply(stranger.avatarUrl));

        // Rounded avatar
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        // Status indicator
        Circle statusIndicator = new Circle(6);
        statusIndicator.getStyleClass().add(getStatusClass(stranger.status));
        StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(statusIndicator, new Insets(0, 0, 2, 0));

        avatarContainer.getChildren().addAll(avatar, statusIndicator);

        // Click avatar to view profile
        String displayName = (stranger.displayName != null && !stranger.displayName.isBlank())
                ? stranger.displayName
                : "Player";
        avatarContainer.setOnMouseClicked(e -> onViewProfile.accept(stranger.id, displayName));

        // Stranger info
        VBox info = new VBox(3);
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("friend-name");

        String statusText = mapStatus(stranger.status);
        Label statusLabel = new Label(statusText);
        statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(stranger.status));
        
        Label eloLabel = new Label("Elo: ...");
        eloLabel.getStyleClass().add("friend-elo-text");
        
        // Load Elo từ API async
        if (stranger.id != null) {
            new Thread(() -> {
                try {
                    Integer elo = UserApi.getUserElo(stranger.id);
                    Platform.runLater(() -> {
                        eloLabel.setText("Elo: " + (elo != null ? elo : 0));
                    });
                } catch (Exception e) {
                    System.err.println("[StrangerItem] Failed to load Elo for user " + stranger.id + ": " + e.getMessage());
                    Platform.runLater(() -> {
                        eloLabel.setText("Elo: 0");
                    });
                }
            }).start();
        }

        HBox statusEloBox = new HBox(5);
        statusEloBox.setAlignment(Pos.CENTER_LEFT);
        statusEloBox.getChildren().addAll(statusLabel, eloLabel);

        info.getChildren().addAll(nameLabel, statusEloBox);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Add Friend icon (for strangers) - only show if no outgoing request
        if (!hasOutgoingRequest && onSendFriendRequest != null) {
            ImageView addIcon = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResource("/com/example/memorygame/assets/images/icon/plus.png")).toExternalForm()));
            addIcon.setFitWidth(24);
            addIcon.setFitHeight(24);
            addIcon.setPreserveRatio(true);

            StackPane addBtn = new StackPane(addIcon);
            addBtn.getStyleClass().add("icon-button-main");
            addBtn.setCursor(Cursor.HAND);
            addBtn.setOnMouseClicked(e -> {
                onSendFriendRequest.accept(stranger.id, displayName);
                // Hide button after sending request
                addBtn.setVisible(false);
                addBtn.setManaged(false);
            });

            row.getChildren().addAll(avatarContainer, info, addBtn);
        } else {
            // No add button if already sent request
            row.getChildren().addAll(avatarContainer, info);
        }
        return row;
    }

    private String getStatusClass(String status) {
        if (status == null)
            return "status-offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "status-online";
            case "BUSY", "IN_GAME" -> "status-busy";
            default -> "status-offline";
        };
    }

    private String mapStatus(String status) {
        if (status == null)
            return "Offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "Online";
            case "BUSY", "IN_GAME" -> "Busy";
            default -> "Offline";
        };
    }

    private String getStatusTextClass(String status) {
        if (status == null)
            return "status-offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "status-online";
            case "BUSY", "IN_GAME" -> "status-busy";
            default -> "status-offline";
        };
    }

}
