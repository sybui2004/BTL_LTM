package com.example.memorygame.controller.friend;

import com.example.memorygame.controller.room.RoomStateManager;
import com.example.memorygame.controller.room.RoomUIUpdater;
import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.UserSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds UI components for friend list items
 */
public class FriendItemBuilder {
    private final RoomStateManager stateManager;
    private final RoomUIUpdater.ImageLoader imageLoader;
    private final Consumer<Long> onInviteUser;
    
    public FriendItemBuilder(RoomStateManager stateManager, 
                            RoomUIUpdater.ImageLoader imageLoader,
                            Consumer<Long> onInviteUser) {
        this.stateManager = stateManager;
        this.imageLoader = imageLoader;
        this.onInviteUser = onInviteUser;
    }
    
    public HBox createFriendItem(UserSummary user, int index) {
        HBox row = new HBox(10);
        row.getStyleClass().add("friend-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        avatar.setImage(imageLoader.loadUserAvatarOrFallback(user.avatarUrl));
        
        VBox texts = new VBox(2);
        String display = (user.displayName != null && !user.displayName.isBlank()) 
                        ? user.displayName 
                        : (user.username != null ? user.username : "Player");
        Label name = new Label(display);
        name.getStyleClass().add("friend-name");
        String statusText = (user.status != null) ? mapStatus(user.status) : "";
        Label status = new Label(statusText);
        status.getStyleClass().addAll("status", statusClass(statusText));
        texts.getChildren().addAll(name, status);
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        ImageView plusIcon = new ImageView(new Image(Objects.requireNonNull(
            getClass().getResource("/com/example/memorygame/assets/images/icon/plus.png")).toExternalForm()));
        plusIcon.setFitWidth(20);
        plusIcon.setFitHeight(20);
        plusIcon.setPreserveRatio(true);
        
        StackPane plusBtn = new StackPane(plusIcon);
        plusBtn.getStyleClass().add("icon-button");
        HBox.setMargin(plusBtn, new Insets(0, 8, 0, 0));
        
        plusBtn.setOnMouseClicked(e -> onInviteUser.accept(user.id));
        
        // Hide + button logic
        boolean isOffline = user.status == null || "OFFLINE".equalsIgnoreCase(user.status.trim());
        boolean roomIsFull = stateManager.isRoomFull();
        if ((stateManager.getCurrentGuestId() != null && stateManager.getCurrentGuestId().equals(user.id)) ||
            (stateManager.getCurrentHostId() != null && stateManager.getCurrentHostId().equals(user.id)) ||
            isOffline ||
            roomIsFull) {
            plusBtn.setVisible(false);
            plusBtn.setManaged(false);
        }
        
        row.getChildren().addAll(avatar, texts, spacer, plusBtn);
        return row;
    }
    
    public HBox createFriendItemFromDTO(FriendDTO friend, int index) {
        HBox row = new HBox(10);
        row.getStyleClass().add("friend-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        avatar.setImage(imageLoader.loadUserAvatarOrFallback(friend.avatarUrl));
        
        VBox texts = new VBox(2);
        String display = (friend.displayName != null && !friend.displayName.isBlank()) 
                        ? friend.displayName 
                        : "Player";
        Label name = new Label(display);
        name.getStyleClass().add("friend-name");
        String statusText = (friend.status != null) ? mapStatus(friend.status) : "";
        Label status = new Label(statusText);
        status.getStyleClass().addAll("status", statusClass(statusText));
        texts.getChildren().addAll(name, status);
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        ImageView plusIcon = new ImageView(new Image(Objects.requireNonNull(
            getClass().getResource("/com/example/memorygame/assets/images/icon/plus.png")).toExternalForm()));
        plusIcon.setFitWidth(20);
        plusIcon.setFitHeight(20);
        plusIcon.setPreserveRatio(true);
        
        StackPane plusBtn = new StackPane(plusIcon);
        plusBtn.getStyleClass().add("icon-button");
        HBox.setMargin(plusBtn, new Insets(0, 8, 0, 0));
        
        plusBtn.setOnMouseClicked(e -> onInviteUser.accept(friend.id));
        
        // Hide + button logic
        boolean isOffline = friend.status == null || "OFFLINE".equalsIgnoreCase(friend.status.trim());
        boolean roomIsFull = stateManager.isRoomFull();
        if ((stateManager.getCurrentGuestId() != null && stateManager.getCurrentGuestId().equals(friend.id)) ||
            (stateManager.getCurrentHostId() != null && stateManager.getCurrentHostId().equals(friend.id)) ||
            isOffline ||
            roomIsFull) {
            plusBtn.setVisible(false);
            plusBtn.setManaged(false);
        }
        
        row.getChildren().addAll(avatar, texts, spacer, plusBtn);
        return row;
    }
    
    private String statusClass(String statusText) {
        String s = statusText == null ? "" : statusText.toLowerCase();
        if (s.contains("busy")) return "busy";
        if (s.contains("online")) return "online";
        return "offline";
    }
    
    private String mapStatus(String backendStatus) {
        if (backendStatus == null) return "Offline";
        String s = backendStatus.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "Online";
            case "BUSY" -> "Busy";
            default -> "Offline";
        };
    }
}

