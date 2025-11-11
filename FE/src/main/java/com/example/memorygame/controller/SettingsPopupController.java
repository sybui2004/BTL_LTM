package com.example.memorygame.controller;

import com.example.memorygame.model.user.UserSettingDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.utils.TCPClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for Settings Popup - reusable across MainScreen, RoomScreen, GameScreen
 */
public class SettingsPopupController {
    
    @FXML
    private VBox settingsPopup;
    
    @FXML
    private Slider backgroundMusicSlider;
    
    @FXML
    private Label backgroundMusicValue;
    
    @FXML
    private Slider soundFxSlider;
    
    @FXML
    private Label soundFxValue;
    
    @FXML
    private CheckBox notificationsCheckbox;
    
    private boolean isLoadingSettings = false;
    private UserSummary currentUser;
    
    // References to parent overlay (set by parent controllers)
    private javafx.scene.layout.StackPane popupOverlay;
    
    @FXML
    private void initialize() {
        setupSettingsListeners();
    }
    
    /**
     * Set the current user for loading/saving settings
     */
    public void setCurrentUser(UserSummary user) {
        this.currentUser = user;
    }
    
    /**
     * Set references to parent overlay
     * Called by parent controllers (MainScreen, RoomScreen, GameScreen)
     */
    public void setParentOverlay(javafx.scene.layout.StackPane overlay) {
        this.popupOverlay = overlay;
    }
    
    /**
     * Show the settings popup and load settings
     */
    public void show() {
        if (settingsPopup != null) {
            System.out.println("[SettingsPopup] Showing popup");
            settingsPopup.setVisible(true);
            settingsPopup.setManaged(true);
            // Ensure popup is on top
            if (settingsPopup.getParent() != null) {
                settingsPopup.toFront();
            }
            
            // Show parent overlay if available
            if (popupOverlay != null) {
                popupOverlay.setVisible(true);
                popupOverlay.setManaged(true);
                popupOverlay.setMouseTransparent(false);
            }
            
            loadSettings();
        } else {
            System.err.println("[SettingsPopup] settingsPopup VBox is null!");
        }
    }
    
    /**
     * Hide the settings popup
     */
    public void hide() {
        if (settingsPopup != null) {
            settingsPopup.setVisible(false);
            settingsPopup.setManaged(false);
        }
        
        // Hide parent overlay if available
        if (popupOverlay != null) {
            popupOverlay.setVisible(false);
            popupOverlay.setManaged(false);
            popupOverlay.setMouseTransparent(true);
        }
    }
    
    /**
     * Toggle the settings popup visibility
     */
    public void toggle() {
        if (settingsPopup != null && settingsPopup.isVisible()) {
            hide();
        } else {
            show();
        }
    }
    
    @FXML
    private void closePopup() {
        hide();
    }
    
    private void loadSettings() {
        if (currentUser == null) {
            currentUser = UserApi.getCurrentUser();
        }
        if (currentUser == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                UserSettingDTO settings = UserApi.getSettings(currentUser.id);
                if (settings != null) {
                    Platform.runLater(() -> {
                        isLoadingSettings = true;
                        if (backgroundMusicSlider != null) {
                            backgroundMusicSlider.setValue(settings.musicVolume);
                            if (backgroundMusicValue != null) {
                                backgroundMusicValue.setText(String.valueOf((int) settings.musicVolume));
                            }
                            // Apply loaded volume to SoundManager
                            SoundManager.setBackgroundMusicVolume(settings.musicVolume / 100.0);
                        }
                        if (soundFxSlider != null) {
                            soundFxSlider.setValue(settings.soundFxVolume);
                            if (soundFxValue != null) {
                                soundFxValue.setText(String.valueOf((int) settings.soundFxVolume));
                            }
                            // Apply loaded volume to SoundManager
                            SoundManager.setSoundFxVolume(settings.soundFxVolume / 100.0);
                        }
                        if (notificationsCheckbox != null) {
                            notificationsCheckbox.setSelected(settings.notification);
                        }
                        // Update SoundManager notification enabled state
                        SoundManager.setNotificationEnabled(settings.notification);
                        isLoadingSettings = false;
                    });
                }
            } catch (Exception e) {
                System.err.println("Failed to load settings: " + e.getMessage());
            }
        }).start();
    }
    
    private void saveSettings() {
        if (isLoadingSettings || currentUser == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                Integer musicVolume = backgroundMusicSlider != null ? (int) backgroundMusicSlider.getValue() : null;
                Integer soundFxVolume = soundFxSlider != null ? (int) soundFxSlider.getValue() : null;
                Boolean notificationEnabled = notificationsCheckbox != null ? notificationsCheckbox.isSelected() : null;
                
                UserSettingDTO updated = UserApi.updateSettings(currentUser.id, musicVolume, soundFxVolume, notificationEnabled);
                if (updated != null) {
                    System.out.println("Settings saved successfully");
                }
            } catch (Exception e) {
                System.err.println("Failed to save settings: " + e.getMessage());
            }
        }).start();
    }
    
    private void setupSettingsListeners() {
        if (backgroundMusicSlider != null) {
            backgroundMusicSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int value = newVal.intValue();
                if (backgroundMusicValue != null) {
                    backgroundMusicValue.setText(String.valueOf(value));
                }
                // Apply volume change in realtime (convert 0-100 to 0.0-1.0)
                double volume = value / 100.0;
                SoundManager.setBackgroundMusicVolume(volume);
                saveSettings();
            });
        }
        
        if (soundFxSlider != null) {
            soundFxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int value = newVal.intValue();
                if (soundFxValue != null) {
                    soundFxValue.setText(String.valueOf(value));
                }
                // Apply volume change in realtime (convert 0-100 to 0.0-1.0)
                double volume = value / 100.0;
                SoundManager.setSoundFxVolume(volume);
                saveSettings();
            });
        }
        
        if (notificationsCheckbox != null) {
            notificationsCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                // Update SoundManager immediately when checkbox changes
                SoundManager.setNotificationEnabled(newVal);
                saveSettings();
            });
        }
    }
    
    /**
     * Get the settings popup node
     */
    public VBox getSettingsPopup() {
        return settingsPopup;
    }
    
    /**
     * Handle logout action (called from logout button in settings popup)
     */
    @FXML
    private void handleLogout() {
        SoundManager.playSound("button.wav");
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.setContentText("You will need to login again to play.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Disconnect TCP
                    TCPClient.getInstance().disconnect();
                    
                    // Clear token
                    TokenManager.getInstance().clearToken();
                    
                    // Navigate to Auth screen
                    AuthScreenController authController = new AuthScreenController();
                    javafx.scene.Scene scene = new javafx.scene.Scene(authController.getScreen().getRoot());
                    
                    // Get stage from settings popup
                    Stage stage = null;
                    if (settingsPopup != null && settingsPopup.getScene() != null) {
                        stage = (Stage) settingsPopup.getScene().getWindow();
                    }
                    
                    if (stage != null) {
                        stage.setScene(scene);
                    } else {
                        // Fallback: try to get any visible stage
                        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                            if (window instanceof Stage && window.isShowing()) {
                                stage = (Stage) window;
                                stage.setScene(scene);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[SettingsPopup] Failed to logout: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}

