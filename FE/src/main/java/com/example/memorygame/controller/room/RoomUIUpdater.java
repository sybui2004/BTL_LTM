package com.example.memorygame.controller.room;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Updates UI elements for room (host/guest avatars, play button)
 */
public class RoomUIUpdater {
    private final ImageView hostAvatar;
    private final ImageView guestAvatar;
    private final Label hostStatus;
    private final Label guestStatus;
    private final Label guestPlaceholder;
    private final StackPane playButton;
    private final ImageLoader imageLoader;
    
    public RoomUIUpdater(ImageView hostAvatar, ImageView guestAvatar,
                        Label hostStatus, Label guestStatus, Label guestPlaceholder,
                        StackPane playButton, ImageLoader imageLoader) {
        this.hostAvatar = hostAvatar;
        this.guestAvatar = guestAvatar;
        this.hostStatus = hostStatus;
        this.guestStatus = guestStatus;
        this.guestPlaceholder = guestPlaceholder;
        this.playButton = playButton;
        this.imageLoader = imageLoader;
    }
    
    public void updateGuestInfo(String guestDisplayName, String guestAvatarUrl) {
        if (guestStatus != null) {
            guestStatus.setText(guestDisplayName);
        }
        
        if (guestAvatar != null) {
            Image image = imageLoader.loadUserAvatarOrFallback(guestAvatarUrl);
            guestAvatar.setImage(image);
            guestAvatar.setVisible(true);
        }
        
        if (guestPlaceholder != null) {
            guestPlaceholder.setVisible(false);
        }
    }
    
    public void updateHostInfo(String hostDisplayName, String hostAvatarUrl) {
        if (hostStatus != null) {
            hostStatus.setText(hostDisplayName);
        }
        
        if (hostAvatar != null) {
            Image image = imageLoader.loadUserAvatarOrFallback(hostAvatarUrl);
            hostAvatar.setImage(image);
        }
    }
    
    public void clearGuestInfo() {
        if (guestStatus != null) {
            guestStatus.setText("Waiting for player...");
        }
        if (guestAvatar != null) {
            guestAvatar.setVisible(false);
        }
        if (guestPlaceholder != null) {
            guestPlaceholder.setVisible(true);
        }
    }
    
    public void setPlayButtonEnabled(boolean enabled) {
        if (playButton != null) {
            playButton.setDisable(!enabled);
            if (enabled) {
                playButton.getStyleClass().remove("cta-btn-disabled");
                playButton.setOpacity(1.0);
            } else {
                if (!playButton.getStyleClass().contains("cta-btn-disabled")) {
                    playButton.getStyleClass().add("cta-btn-disabled");
                }
                playButton.setOpacity(0.5);
            }
        }
    }
    
    /**
     * Interface for loading images (to avoid circular dependency)
     */
    public interface ImageLoader {
        Image loadUserAvatarOrFallback(String candidateUrl);
    }
}

