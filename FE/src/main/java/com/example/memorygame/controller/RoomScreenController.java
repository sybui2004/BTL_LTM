package com.example.memorygame.controller;

import com.example.memorygame.view.RoomScreen;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RoomScreenController {
    private final RoomScreen screen;
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
    @FXML private StackPane questionButton;
    @FXML private StackPane popupOverlay;
    @FXML private Button closePopupButton;

    private enum Tab { FRIENDS, STRANGERS, RECENT }
    private final ToggleGroup tabsGroup = new ToggleGroup();

    public RoomScreenController() {
        this.screen = new RoomScreen(this);
    }

    public RoomScreen getScreen() {
        return screen;
    }

    @FXML
    private void initialize() {
        // Try to load custom title font
        try {
            Font loaded = Font.loadFont(getClass().getResourceAsStream("/com/example/memorygame/assets/fonts/PlaywriteDESAS-VariableFont_wght.ttf"), 26);
            if (loaded != null && titleLabel != null) {
                String fam = loaded.getFamily();
                titleLabel.setFont(Font.font(fam, FontWeight.BOLD, 26));
                titleLabel.setStyle("-fx-font-family: '" + fam + "'; -fx-font-size: 26px; -fx-font-weight: bold;");
            }
        } catch (Exception ignored) { }
        setupTabs();
        if (btnSearch != null) btnSearch.setOnAction(e -> handleSearch());
        if (txtSearch != null) txtSearch.setOnAction(e -> handleSearch());
        setupRoomAvatars();
        setupPopup();
        switchTab(Tab.FRIENDS);
    }

    private void setupTabs() {
        tabFriends.setToggleGroup(tabsGroup);
        tabStrangers.setToggleGroup(tabsGroup);
        tabRecent.setToggleGroup(tabsGroup);
        tabsGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) return;
            if (newT == tabFriends) switchTab(Tab.FRIENDS);
            else if (newT == tabStrangers) switchTab(Tab.STRANGERS);
            else switchTab(Tab.RECENT);
        });
        tabFriends.setSelected(true);
    }

    private void switchTab(Tab tab) {
        if (tab == Tab.STRANGERS) {
            setSearchVisible(true);
            listContainer.getChildren().clear();
            return;
        }

        setSearchVisible(false);
        new Thread(() -> {
            if (tab == Tab.FRIENDS) {
                var user = UserApi.getUserById(1L); // temporary: always user id 1
                java.util.List<UserSummary> one = (user == null) ? java.util.List.of() : java.util.List.of(user);
                Platform.runLater(() -> populateList(one));
            } else { // RECENT tab
                var users = UserApi.getAllUsers();
                Platform.runLater(() -> populateList(users));
            }
        }).start();
    }

    private void handleSearch() {
        String text = txtSearch.getText();
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        long id;
        try {
            id = Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            listContainer.getChildren().setAll(new Label("Invalid id"));
            return;
        }
        listContainer.getChildren().setAll(new Label("Searching..."));
        new Thread(() -> {
            var user = UserApi.getUserById(id);
            Platform.runLater(() -> {
                if (user == null) {
                    listContainer.getChildren().setAll(new Label("No user found"));
                } else {
                    populateList(java.util.List.of(user));
                }
            });
        }).start();
    }

    private void setSearchVisible(boolean visible) {
        searchContainer.setVisible(visible);
        searchContainer.setManaged(visible);
    }

    private void populateList(java.util.List<UserSummary> users) {
        listContainer.getChildren().clear();
        for (int i = 0; i < users.size(); i++) {
            UserSummary u = users.get(i);
            listContainer.getChildren().add(createFriendItem(u, i));
        }
    }

    private HBox createFriendItem(UserSummary user, int index) {
        HBox row = new HBox(10);
        row.getStyleClass().add("friend-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8,10,8,10));

        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        avatar.setImage(loadUserAvatarOrFallback(user.avatarUrl));

        VBox texts = new VBox(2);
        String display = (user.displayName != null && !user.displayName.isBlank()) ? user.displayName : (user.username != null ? user.username : "Player");
        Label name = new Label(display);
        name.getStyleClass().add("friend-name");
        String statusText = (user.status != null) ? mapStatus(user.status) : "";
        Label status = new Label(statusText);
        status.getStyleClass().addAll("status", statusClass(statusText));
        texts.getChildren().addAll(name, status);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        ImageView plusIcon = new ImageView(new Image(getClass().getResource("/com/example/memorygame/assets/images/icon/plus.png").toExternalForm()));
        plusIcon.setFitWidth(20);
        plusIcon.setFitHeight(20);
        plusIcon.setPreserveRatio(true);

        javafx.scene.layout.StackPane plusBtn = new javafx.scene.layout.StackPane(plusIcon);
        plusBtn.getStyleClass().add("icon-button");
        HBox.setMargin(plusBtn, new Insets(0, 8, 0, 0)); // pull away from right edge a bit

        row.getChildren().addAll(avatar, texts, spacer, plusBtn);
        return row;
    }


    private String statusClass(String statusText) {
        String s = statusText == null ? "" : statusText.toLowerCase();
        if (s.contains("busy")) return "busy";
        if (s.contains("online")) return "online";
        return "offline";
    }

    private Image loadUserAvatarOrFallback(String candidateUrl) {
        // Fallback image packaged in resources
        String fallbackResource = "/com/example/memorygame/assets/images/name.png";
        try {
            if (candidateUrl != null) {
                String trimmed = candidateUrl.trim();
                if (!trimmed.isEmpty()) {
                    // Support absolute http(s) URLs only; other forms will fallback to resource
                    String lower = trimmed.toLowerCase();
                    if (lower.startsWith("http://") || lower.startsWith("https://")) {
                        return new Image(trimmed, true);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Will fallback below
        }
        var url = getClass().getResource(fallbackResource);
        return new Image(url.toExternalForm(), true);
    }

    private String mapStatus(String backendStatus) {
        if (backendStatus == null) return "Offline";
        String s = backendStatus.trim().toUpperCase();
        switch (s) {
            case "ONLINE": return "Online";
            case "BUSY": return "Busy"; // map BUSY to ingame styling
            case "OFFLINE":
            default: return "Offline";
        }
    }

    private void setupRoomAvatars() {
        // Sample host avatar - you can replace with actual user data later
        if (hostAvatar != null) {
            hostAvatar.setVisible(true);
            hostAvatar.setImage(new Image(getClass().getResource("/com/example/memorygame/assets/images/name.png").toExternalForm()));
            
            // Tạo clip để bo góc khớp kích thước hiển thị (96x96 cho host)
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(96, 96);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            hostAvatar.setClip(clip);
        }
        if (hostStatus != null) {
            hostStatus.setText("John Doe"); // Sample host name
        }
        
        // Guest slot remains empty for now
        if (guestAvatar != null) {
            guestAvatar.setVisible(false); // Keep hidden until guest joins
            
            // Tạo clip để bo góc cho guest khớp kích thước hiển thị (96x96)
            javafx.scene.shape.Rectangle guestClip = new javafx.scene.shape.Rectangle(96, 96);
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
            questionButton.setOnMouseClicked(e -> showPopup());
        }
        if (closePopupButton != null) {
            closePopupButton.setOnAction(e -> hidePopup());
        }
        if (popupOverlay != null) {
            popupOverlay.setOnMouseClicked(e -> {
                // Close popup when clicking on the overlay background
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
}
