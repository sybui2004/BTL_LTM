package com.example.memorygame.controller.room;

import com.example.memorygame.model.game.InviteDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

/**
 * Builds UI components for invite list items
 */
public class InviteItemBuilder {
    private final RoomUIUpdater.ImageLoader imageLoader;
    private final Consumer<InviteDTO> onAcceptInvite;
    private final Consumer<InviteDTO> onRejectInvite;
    
    public InviteItemBuilder(RoomUIUpdater.ImageLoader imageLoader,
                            Consumer<InviteDTO> onAcceptInvite,
                            Consumer<InviteDTO> onRejectInvite) {
        this.imageLoader = imageLoader;
        this.onAcceptInvite = onAcceptInvite;
        this.onRejectInvite = onRejectInvite;
    }
    
    public HBox createInviteItem(InviteDTO invite) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("invite-item");
        item.setPadding(new Insets(10, 12, 10, 12));
        
        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(false);
        
        // Load sender avatar in background
        new Thread(() -> {
            try {
                UserSummary sender = UserApi.getUserById(invite.senderId);
                Platform.runLater(() -> {
                    if (sender != null && sender.avatarUrl != null) {
                        avatar.setImage(imageLoader.loadUserAvatarOrFallback(sender.avatarUrl));
                    } else {
                        avatar.setImage(imageLoader.loadUserAvatarOrFallback(null));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> avatar.setImage(imageLoader.loadUserAvatarOrFallback(null)));
            }
        }).start();
        
        // Clip avatar to circle
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);
        
        // Sender info
        VBox textBox = new VBox(3);
        Label senderLabel = new Label(invite.senderName);
        senderLabel.getStyleClass().add("invite-sender");
        Label inviteText = new Label("mời bạn vào phòng");
        inviteText.getStyleClass().add("invite-text");
        textBox.getChildren().addAll(senderLabel, inviteText);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
        
        // Buttons container
        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        
        // Accept button
        Button acceptBtn = new Button("✓");
        acceptBtn.getStyleClass().addAll("invite-btn", "invite-accept-btn");
        acceptBtn.setOnAction(e -> {
            SoundManager.playSound("button.wav");
            onAcceptInvite.accept(invite);
        });
        setButtonSize(acceptBtn);
        
        // Reject button
        Button rejectBtn = new Button("✕");
        rejectBtn.getStyleClass().addAll("invite-btn", "invite-reject-btn");
        rejectBtn.setOnAction(e -> {
            SoundManager.playSound("button.wav");
            onRejectInvite.accept(invite);
        });
        setButtonSize(rejectBtn);
        
        buttonsBox.getChildren().addAll(acceptBtn, rejectBtn);
        
        item.getChildren().addAll(avatar, textBox, buttonsBox);
        return item;
    }
    
    private void setButtonSize(Button button) {
        button.setMinWidth(40);
        button.setPrefWidth(40);
        button.setMaxWidth(40);
        button.setMinHeight(40);
        button.setPrefHeight(40);
        button.setMaxHeight(40);
    }
}

