package com.example.memorygame.controller;

import com.example.memorygame.model.game.MatchHistoryDTO;
import com.example.memorygame.model.game.MatchHistoryRow;
import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.FriendListDTO;
import com.example.memorygame.model.user.UserProfileDTO;
import com.example.memorygame.utils.FriendApi;
import com.example.memorygame.utils.JwtDecoder;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.TokenManager;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.view.ProfileScreen;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * Controller for User Profile Screen
 */
public class ProfileScreenController {
    private final ProfileScreen screen;

    @FXML
    private ImageView avatarImageView;
    @FXML
    private javafx.scene.shape.Circle avatarEditOverlay;
    @FXML
    private Label displayNameLabel;
    @FXML
    private TextField displayNameField;
    @FXML
    private Label totalMatchesLabel;
    @FXML
    private Label eloLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label friendsLabel;
    @FXML
    private Label joinDateLabel;
    @FXML
    private HBox backButton;
    @FXML
    private TableView<MatchHistoryRow> matchHistoryTable;
    @FXML
    private TableColumn<MatchHistoryRow, String> opponentColumn;
    @FXML
    private TableColumn<MatchHistoryRow, String> resultColumn;
    @FXML
    private TableColumn<MatchHistoryRow, String> eloChangeColumn;
    @FXML
    private TableColumn<MatchHistoryRow, String> dateColumn;
    @FXML
    private VBox friendActionContainer;
    @FXML
    private Button addFriendButton;
    @FXML
    private Button removeFriendButton;
    @FXML
    private VBox editButtonContainer;
    @FXML
    private Button editButton;

    private boolean isEditMode = false;
    private String originalDisplayName;
    private String originalAvatarUrl;
    private String selectedAvatarUrl;
    private String currentEmail;

    private Long userId;
    private Long currentUserId;
    private String relationshipStatus; // "self", "friend", "none", "pending_incoming", "pending_outgoing"
    private final ObservableList<MatchHistoryRow> matchHistoryData = FXCollections.observableArrayList();

    public ProfileScreenController(Long userId) {
        this.userId = userId;
        try {
            this.screen = new ProfileScreen(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ProfileScreen", e);
        }
    }

    @FXML
    private void initialize() {
        setupBackButton();
        setupTable();
        setupTCPListeners();
        loadProfile();
    }

    private void setupTCPListeners() {
        TCPClient client = TCPClient.getInstance();

        // Listen for friend status changes (accept/reject/remove) - refresh button
        client.onMessage("FRIEND_STATUS_CHANGED", message -> {
            System.out.println("[Profile] Received friend status changed notification via TCP");
            // Reload relationship status and update buttons
            if (currentUserId != null && userId != null && !currentUserId.equals(userId)) {
                new Thread(() -> {
                    String relationship = checkRelationship(currentUserId);
                    Platform.runLater(() -> setupFriendButtons(relationship));
                }).start();
            }
        });
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnMouseClicked(e -> goBack());
        }
    }

    private void setupTable() {
        if (matchHistoryTable == null)
            return;

        matchHistoryTable.setItems(matchHistoryData);

        // Set column resize policy to fill available width
        matchHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        opponentColumn.setCellValueFactory(new PropertyValueFactory<>("opponent"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
        eloChangeColumn.setCellValueFactory(new PropertyValueFactory<>("eloChange"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Custom cell factories for styling and alignment - Căn giữa các cột Result,
        // Elo, Date
        resultColumn.setCellFactory(column -> new TableCell<MatchHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setAlignment(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER); // Căn giữa
                    if ("WIN".equals(item)) {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if ("LOSE".equals(item)) {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-alignment: center;");
                    } else {
                        setStyle("-fx-text-fill: #F39C12; -fx-alignment: center;");
                    }
                }
            }
        });

        eloChangeColumn.setCellFactory(column -> new TableCell<MatchHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setAlignment(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER); // Căn giữa
                    if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-alignment: center;");
                    } else {
                        setStyle("-fx-alignment: center;");
                    }
                }
            }
        });

        dateColumn.setCellFactory(column -> new TableCell<MatchHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setAlignment(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER); // Căn giữa
                    setStyle("-fx-alignment: center;");
                }
            }
        });
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                // Get current user ID
                String token = TokenManager.getInstance().getToken();
                if (token != null) {
                    currentUserId = JwtDecoder.extractUserId(token);
                }

                // Load profile data
                UserProfileDTO profile = UserApi.getUserProfile(userId);
                if (profile == null) {
                    Platform.runLater(() -> {
                        showAlert("Unable to load profile information", Alert.AlertType.ERROR);
                        goBack();
                    });
                    return;
                }

                // Debug: Log match history
                System.out.println("[Profile] Loaded profile for user: " + userId);
                System.out.println("[Profile] Match history count: "
                        + (profile.matchHistory != null ? profile.matchHistory.size() : 0));
                if (profile.matchHistory != null && !profile.matchHistory.isEmpty()) {
                    System.out.println("[Profile] First match: " + profile.matchHistory.get(0).opponentUsername + " - "
                            + profile.matchHistory.get(0).result);
                }

                // Load Elo
                Integer elo = UserApi.getUserElo(userId);

                // Load friends count (for the profile user)
                FriendListDTO friendList = FriendApi.getFriendList();
                final int friendsCount = (friendList != null && friendList.friends != null)
                        ? friendList.friends.size()
                        : 0;

                // Load username and email
                var userSummary = UserApi.getUserById(userId);
                final String username = userSummary != null && userSummary.username != null ? userSummary.username
                        : "Unknown";

                // Get full user details for email
                var userDetails = UserApi.getUserDetailsById(userId);
                final String email = userDetails != null && userDetails.email != null ? userDetails.email : "";
                currentEmail = email;

                // Check relationship if viewing other user's profile
                String relationship = "none";
                if (currentUserId != null && !currentUserId.equals(userId)) {
                    relationship = checkRelationship(currentUserId);
                } else if (currentUserId != null && currentUserId.equals(userId)) {
                    relationship = "self";
                }
                final String finalRelationship = relationship;

                Platform.runLater(() -> {
                    displayProfile(profile, elo, friendsCount, username, email);
                    setupFriendButtons(finalRelationship);
                });
            } catch (Exception e) {
                System.err.println("[Profile] Error loading profile: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error loading profile: " + e.getMessage(), Alert.AlertType.ERROR);
                    goBack();
                });
            }
        }).start();
    }

    private String checkRelationship(Long currentUserId) {
        try {
            FriendListDTO friendList = FriendApi.getFriendList();
            if (friendList == null) {
                return "none";
            }

            // Check if already friends
            if (friendList.friends != null) {
                for (FriendDTO friend : friendList.friends) {
                    if (friend.id != null && friend.id.equals(userId)) {
                        return "friend";
                    }
                }
            }

            // Check if incoming request (they sent to me)
            if (friendList.incomingRequest != null) {
                for (FriendDTO request : friendList.incomingRequest) {
                    if (request.id != null && request.id.equals(userId)) {
                        return "pending_incoming";
                    }
                }
            }

            // Check if outgoing request (I sent to them)
            if (friendList.outgoingRequest != null) {
                for (FriendDTO request : friendList.outgoingRequest) {
                    if (request.id != null && request.id.equals(userId)) {
                        return "pending_outgoing";
                    }
                }
            }

            return "none";
        } catch (Exception e) {
            System.err.println("[Profile] Error checking relationship: " + e.getMessage());
            return "none";
        }
    }

    private void setupFriendButtons(String relationship) {
        if (friendActionContainer == null || addFriendButton == null || removeFriendButton == null) {
            return;
        }

        relationshipStatus = relationship;

        if ("self".equals(relationship)) {
            // Show edit button for own profile
            if (editButtonContainer != null) {
                editButtonContainer.setVisible(true);
                editButtonContainer.setManaged(true);
            }
            // Hide friend action buttons
            friendActionContainer.setVisible(false);
            friendActionContainer.setManaged(false);
        } else {
            // Hide edit button for other profiles
            if (editButtonContainer != null) {
                editButtonContainer.setVisible(false);
                editButtonContainer.setManaged(false);
            }
            // Show appropriate button
            friendActionContainer.setVisible(true);
            friendActionContainer.setManaged(true);

            if ("friend".equals(relationship)) {
                // Show "Remove Friend"
                addFriendButton.setVisible(false);
                addFriendButton.setManaged(false);
                removeFriendButton.setVisible(true);
                removeFriendButton.setManaged(true);
            } else if ("pending_outgoing".equals(relationship)) {
                // Hide both buttons when request already sent
                addFriendButton.setVisible(false);
                addFriendButton.setManaged(false);
                removeFriendButton.setVisible(false);
                removeFriendButton.setManaged(false);
            } else {
                // Show "Add Friend" (for "none", "pending_incoming")
                addFriendButton.setVisible(true);
                addFriendButton.setManaged(true);
                removeFriendButton.setVisible(false);
                removeFriendButton.setManaged(false);
            }
        }
    }

    @FXML
    private void handleAddFriend() {
        if (currentUserId == null || userId == null) {
            showAlert("Không thể gửi lời mời kết bạn", Alert.AlertType.ERROR);
            return;
        }

        new Thread(() -> {
            try {
                boolean success = FriendApi.sendFriendRequest(currentUserId, userId);
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Friend request sent", Alert.AlertType.INFORMATION);
                        // Reload relationship status
                        String relationship = checkRelationship(currentUserId);
                        relationshipStatus = relationship;
                        setupFriendButtons(relationshipStatus);
                    } else {
                        showAlert("Unable to send friend request", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Profile] Error sending friend request: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Error sending request: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void handleRemoveFriend() {
        if (currentUserId == null || userId == null) {
            showAlert("Không thể hủy kết bạn", Alert.AlertType.ERROR);
            return;
        }

        new Thread(() -> {
            try {
                boolean success = FriendApi.removeFriend(currentUserId, userId);
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Friend removed", Alert.AlertType.INFORMATION);
                        // Reload relationship status
                        String relationship = checkRelationship(currentUserId);
                        relationshipStatus = relationship;
                        setupFriendButtons(relationshipStatus);
                    } else {
                        showAlert("Unable to remove friend", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Profile] Error removing friend: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Error removing friend: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void handleEditProfile() {
        // Open edit profile dialog popup
        showEditProfileDialog();
    }

    private void enterEditMode() {
        isEditMode = true;
        originalDisplayName = displayNameLabel.getText();
        originalAvatarUrl = selectedAvatarUrl != null ? selectedAvatarUrl : "";

        // Đảm bảo TextField có cùng size với Label để không bị layout shift
        if (displayNameLabel != null && displayNameField != null) {
            // Copy width từ label
            double labelWidth = displayNameLabel.getWidth();
            if (labelWidth > 0) {
                displayNameField.setPrefWidth(labelWidth);
            } else {
                // Fallback nếu label chưa có width
                displayNameField.setPrefWidth(250);
            }

            // Copy height từ label
            double labelHeight = displayNameLabel.getHeight();
            if (labelHeight > 0) {
                displayNameField.setPrefHeight(labelHeight);
            }
        }

        // Switch to edit mode
        displayNameLabel.setVisible(false);
        displayNameLabel.setManaged(false);
        displayNameField.setVisible(true);
        displayNameField.setManaged(true);
        displayNameField.setText(originalDisplayName);

        // Show avatar edit overlay
        if (avatarEditOverlay != null) {
            avatarEditOverlay.setVisible(true);
            avatarEditOverlay.setManaged(true);
            avatarImageView.setOnMouseClicked(e -> showAvatarSelectionDialog());
            avatarImageView.setCursor(javafx.scene.Cursor.HAND);
        }

        // Change button text
        if (editButton != null) {
            editButton.setText("Save");
        }
    }

    private void exitEditMode() {
        isEditMode = false;

        // Switch back to view mode
        displayNameLabel.setVisible(true);
        displayNameLabel.setManaged(true);
        displayNameField.setVisible(false);
        displayNameField.setManaged(false);

        // Hide avatar edit overlay
        if (avatarEditOverlay != null) {
            avatarEditOverlay.setVisible(false);
            avatarEditOverlay.setManaged(false);
            avatarImageView.setOnMouseClicked(null);
            avatarImageView.setCursor(javafx.scene.Cursor.DEFAULT);
        }

        // Change button text
        if (editButton != null) {
            editButton.setText("Edit");
        }
    }

    private void showEditProfileDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Edit your profile information");

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 40, 20, 40));

        // Display Name
        Label nameLabel = new Label("Display Name:");
        TextField nameField = new TextField(displayNameLabel.getText());
        nameField.setPrefWidth(300);
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        // Email (read-only for now)
        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField(currentEmail != null ? currentEmail : "");
        emailField.setEditable(false);
        emailField.setStyle("-fx-background-color: #f0f0f0;");
        emailField.setPrefWidth(300);
        grid.add(emailLabel, 0, 1);
        grid.add(emailField, 1, 1);

        // Password (optional - placeholder for future)
        Label passwordLabel = new Label("New Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave empty to keep current");
        passwordField.setPrefWidth(300);
        grid.add(passwordLabel, 0, 2);
        grid.add(passwordField, 1, 2);

        // Avatar selection
        Label avatarLabel = new Label("Avatar:");
        HBox avatarBox = new HBox(10);
        ImageView currentAvatarView = new ImageView(avatarImageView.getImage());
        currentAvatarView.setFitWidth(80);
        currentAvatarView.setFitHeight(80);
        currentAvatarView.setPreserveRatio(true);
        javafx.scene.shape.Rectangle avatarClip = new javafx.scene.shape.Rectangle(80, 80);
        avatarClip.setArcWidth(15);
        avatarClip.setArcHeight(15);
        currentAvatarView.setClip(avatarClip);

        Button selectAvatarBtn = new Button("Select Avatar");
        selectAvatarBtn.setOnAction(e -> {
            String selected = showAvatarSelectionDialog();
            if (selected != null) {
                selectedAvatarUrl = selected;
                Image newAvatar = loadUserAvatarOrFallback(selected);
                currentAvatarView.setImage(newAvatar);
            }
        });

        avatarBox.getChildren().addAll(currentAvatarView, selectAvatarBtn);
        grid.add(avatarLabel, 0, 3);
        grid.add(avatarBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Handle save
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String newDisplayName = nameField.getText().trim();
                String newPassword = passwordField.getText().trim();

                if (newDisplayName.isEmpty()) {
                    showAlert("Display name cannot be empty", Alert.AlertType.WARNING);
                    return null;
                }

                // Save profile changes
                saveProfileChangesFromDialog(newDisplayName, newPassword);
            }
            return null;
        });

        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(400);

        dialog.setOnShown(e -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (dialogStage != null) {
                dialogStage.setResizable(false);
            }
        });

        dialog.showAndWait();
    }

    private String showAvatarSelectionDialog() {
        // Load avatars from resources/assets/images/avartar directory
        // Avatar source locations (in order of priority):
        // 1. Classpath resources (best - works from JAR or IDE)
        // 2. FE/src/main/resources/com/example/memorygame/assets/images/avartar/
        // (SOURCE)
        // 3. FE/target/classes/com/example/memorygame/assets/images/avartar/ (RUNTIME)

        java.util.List<String> avatarFiles = new java.util.ArrayList<>();

        // Method 1: Try loading from classpath resources first (most reliable)
        try {
            // Try to get the resource directory URL
            java.net.URL resourceUrl = getClass().getResource("/com/example/memorygame/assets/images/avartar");
            if (resourceUrl != null) {
                System.out.println("[Profile] Found resource URL: " + resourceUrl);
                System.out.println("[Profile] Protocol: " + resourceUrl.getProtocol());

                if ("file".equals(resourceUrl.getProtocol())) {
                    // File system resource (when running from IDE or exploded JAR)
                    try {
                        java.io.File resourceDir = new java.io.File(resourceUrl.toURI());
                        System.out.println("[Profile] Resource directory (absolute): " + resourceDir.getAbsolutePath());
                        System.out.println("[Profile] Directory exists: " + resourceDir.exists());
                        System.out.println("[Profile] Is directory: " + resourceDir.isDirectory());

                        if (resourceDir.exists() && resourceDir.isDirectory()) {
                            java.io.File[] files = resourceDir
                                    .listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                            if (files != null) {
                                System.out.println("[Profile] Total files in directory: " + files.length);
                                if (files.length > 0) {
                                    System.out.println(
                                            "[Profile] Found " + files.length + " avatars in classpath resources");
                                    for (java.io.File file : files) {
                                        String avatarPath = "/com/example/memorygame/assets/images/avartar/"
                                                + file.getName();
                                        avatarFiles.add(avatarPath);
                                        System.out.println("[Profile] Added avatar: " + avatarPath);
                                    }
                                } else {
                                    System.out.println("[Profile] Directory exists but no PNG files found");
                                }
                            }
                        }
                    } catch (java.net.URISyntaxException e) {
                        System.err.println("[Profile] URI syntax error: " + e.getMessage());
                    }
                } else if ("jar".equals(resourceUrl.getProtocol())) {
                    // JAR resource (when packaged as JAR)
                    System.out.println("[Profile] JAR resource detected - need to list JAR contents");
                    // For JAR, we'd need to use JarFile or list resources differently
                }
            } else {
                System.out.println("[Profile] Resource URL is null - directory may not exist in classpath");
            }
        } catch (Exception e) {
            System.err.println("[Profile] Error loading avatars from classpath: " + e.getMessage());
            e.printStackTrace();
        }

        // Method 2: Try src/resources (relative to project root)
        if (avatarFiles.isEmpty()) {
            java.io.File srcDir = new java.io.File(
                    "FE/src/main/resources/com/example/memorygame/assets/images/avartar");
            System.out.println("[Profile] Checking src dir: " + srcDir.getAbsolutePath());
            if (srcDir.exists() && srcDir.isDirectory()) {
                java.io.File[] files = srcDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null && files.length > 0) {
                    System.out.println("[Profile] Found " + files.length + " avatars in src/resources");
                    for (java.io.File file : files) {
                        avatarFiles.add("/com/example/memorygame/assets/images/avartar/" + file.getName());
                    }
                } else {
                    System.out.println("[Profile] No PNG files found in src/resources/avartar");
                }
            } else {
                System.out.println("[Profile] src/resources/avartar directory does not exist");
            }
        }

        // Method 3: Fallback to target/classes (runtime location)
        if (avatarFiles.isEmpty()) {
            java.io.File targetDir = new java.io.File("FE/target/classes/com/example/memorygame/assets/images/avartar");
            System.out.println("[Profile] Checking target dir: " + targetDir.getAbsolutePath());
            if (targetDir.exists() && targetDir.isDirectory()) {
                java.io.File[] files = targetDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null && files.length > 0) {
                    System.out.println("[Profile] Found " + files.length + " avatars in target/classes");
                    for (java.io.File file : files) {
                        avatarFiles.add("/com/example/memorygame/assets/images/avartar/" + file.getName());
                    }
                } else {
                    System.out.println("[Profile] No PNG files found in target/classes/avartar");
                }
            } else {
                System.out.println("[Profile] target/classes/avartar directory does not exist");
            }
        }

        // Final fallback to default if no avatars found
        if (avatarFiles.isEmpty()) {
            avatarFiles.add("/com/example/memorygame/assets/images/name.png");
        }

        System.out.println("[Profile] Found " + avatarFiles.size() + " avatars to display");
        if (avatarFiles.size() == 1 && avatarFiles.get(0).equals("/com/example/memorygame/assets/images/name.png")) {
            System.out.println("[Profile] WARNING: No avatars found in avartar directory. Using default avatar.");
            System.out.println(
                    "[Profile] Please add avatar PNG files to: FE/src/main/resources/com/example/memorygame/assets/images/avartar/");
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Avatar");
        dialog.setHeaderText("Select new avatar");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        final String[] selected = { null };

        int col = 0;
        int row = 0;
        for (String avatarPath : avatarFiles) {
            ImageView imgView = new ImageView(loadUserAvatarOrFallback(avatarPath));
            imgView.setFitWidth(80);
            imgView.setFitHeight(80);
            imgView.setPreserveRatio(true);
            imgView.setCursor(javafx.scene.Cursor.HAND);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(80, 80);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imgView.setClip(clip);

            String finalPath = avatarPath;
            imgView.setOnMouseClicked(e -> {
                selected[0] = finalPath;
                dialog.setResult(finalPath);
                dialog.close();
            });

            grid.add(imgView, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.getDialogPane().setPrefWidth(350);
        dialog.getDialogPane().setPrefHeight(350);

        dialog.setOnShown(e -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (dialogStage != null) {
                dialogStage.setResizable(false);
            }
        });

        dialog.showAndWait();
        return selected[0];
    }

    private void saveProfileChangesFromDialog(String newDisplayName, String newPassword) {
        String newAvatarUrl = selectedAvatarUrl != null ? selectedAvatarUrl : originalAvatarUrl;

        new Thread(() -> {
            try {
                // Update profile (display name and avatar)
                boolean profileSuccess = UserApi.updateProfile(userId, newDisplayName, newAvatarUrl);

                // Update password if provided
                boolean passwordSuccess = true;
                if (!newPassword.isEmpty()) {
                    passwordSuccess = UserApi.changePassword(userId, newPassword);
                }

                final boolean finalPasswordSuccess = passwordSuccess;
                Platform.runLater(() -> {
                    if (profileSuccess && finalPasswordSuccess) {
                        String message = newPassword.isEmpty()
                                ? "Profile updated successfully"
                                : "Profile and password updated successfully";
                        showAlert(message, Alert.AlertType.INFORMATION);

                        // Update UI immediately - update avatar if changed
                        if (newAvatarUrl != null && !newAvatarUrl.equals(originalAvatarUrl)) {
                            selectedAvatarUrl = newAvatarUrl;
                            originalAvatarUrl = newAvatarUrl;
                            Image newAvatar = loadUserAvatarOrFallback(newAvatarUrl);
                            if (avatarImageView != null) {
                                avatarImageView.setImage(newAvatar);
                            }
                        }

                        // Update display name immediately
                        if (displayNameLabel != null) {
                            displayNameLabel.setText(newDisplayName);
                        }

                        // Reload profile to refresh data
                        loadProfile();
                    } else if (!profileSuccess) {
                        showAlert("Unable to update profile", Alert.AlertType.ERROR);
                    } else {
                        showAlert("Profile updated but failed to change password", Alert.AlertType.WARNING);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Profile] Error updating profile: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Error updating profile: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void saveProfileChanges() {
        String newDisplayName = displayNameField.getText().trim();
        String newAvatarUrl = selectedAvatarUrl != null ? selectedAvatarUrl : originalAvatarUrl;

        if (newDisplayName.isEmpty()) {
            showAlert("Display name cannot be empty", Alert.AlertType.WARNING);
            return;
        }

        new Thread(() -> {
            try {
                boolean success = UserApi.updateProfile(userId, newDisplayName, newAvatarUrl);
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Profile updated successfully", Alert.AlertType.INFORMATION);

                        // Update display name immediately
                        displayNameLabel.setText(newDisplayName);

                        // Update avatar immediately if changed
                        if (newAvatarUrl != null && !newAvatarUrl.equals(originalAvatarUrl)) {
                            originalAvatarUrl = newAvatarUrl;
                            Image newAvatar = loadUserAvatarOrFallback(newAvatarUrl);
                            if (avatarImageView != null) {
                                avatarImageView.setImage(newAvatar);
                            }
                        }

                        exitEditMode();
                        // Reload profile to refresh data
                        loadProfile();
                    } else {
                        showAlert("Unable to update profile", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                System.err.println("[Profile] Error updating profile: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Error updating profile: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void displayProfile(UserProfileDTO profile, Integer elo, int friendsCount, String username, String email) {
        // Avatar
        if (avatarImageView != null) {
            Image avatar = loadUserAvatarOrFallback(profile.avatarUrl);
            avatarImageView.setImage(avatar);
            avatarImageView.setFitWidth(150);
            avatarImageView.setFitHeight(150);
            avatarImageView.setPreserveRatio(true);

            Rectangle clip = new Rectangle(150, 150);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            avatarImageView.setClip(clip);
        }

        // Display name
        if (displayNameLabel != null) {
            displayNameLabel.setText(profile.displayName != null && !profile.displayName.isBlank()
                    ? profile.displayName
                    : username);
        }

        // Total matches
        int totalMatches = profile.matchHistory != null ? profile.matchHistory.size() : 0;
        if (totalMatchesLabel != null) {
            totalMatchesLabel.setText(String.valueOf(totalMatches));
        }

        // Elo
        if (eloLabel != null) {
            eloLabel.setText(String.valueOf(elo != null ? elo : 0));
        }

        // Wins count
        int wins = 0;
        if (profile.matchHistory != null) {
            for (MatchHistoryDTO match : profile.matchHistory) {
                if ("WIN".equals(match.result)) {
                    wins++;
                }
            }
        }
        if (winsLabel != null) {
            winsLabel.setText(String.valueOf(wins));
        }

        // Friends count
        if (friendsLabel != null) {
            friendsLabel.setText(String.valueOf(friendsCount));
        }

        // Join date
        if (joinDateLabel != null && profile.createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            joinDateLabel.setText(sdf.format(profile.createdAt));
        }

        // Match history table
        if (profile.matchHistory != null && !profile.matchHistory.isEmpty()) {
            matchHistoryData.clear();

            for (MatchHistoryDTO match : profile.matchHistory) {
                String opponent = match.opponentUsername != null ? match.opponentUsername : "Unknown";
                String result = "WIN".equals(match.result) ? "WIN" : ("LOSE".equals(match.result) ? "LOSE" : "DRAW");

                // Calculate Elo change based on scores (if available)
                // Default: +10 for win, -10 for loss, 0 for draw
                String eloChange;
                if (match.userScore > match.opponentScore) {
                    eloChange = "+" + (match.userScore - match.opponentScore);
                } else if (match.userScore < match.opponentScore) {
                    eloChange = "-" + (match.opponentScore - match.userScore);
                } else {
                    eloChange = "0";
                }

                // Format date as dd/MM/yyyy
                String date = match.playedAt != null ? new SimpleDateFormat("dd/MM/yyyy").format(match.playedAt) : "--";

                matchHistoryData.add(new MatchHistoryRow(opponent, result, eloChange, date));
            }
        }
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

    private void goBack() {
        try {
            MainScreenController mainController = new MainScreenController();
            Scene scene = new Scene(mainController.getScreen().getRoot());

            Object root = this.screen.getRoot();
            if (root instanceof javafx.scene.Node) {
                Stage stage = (Stage) ((javafx.scene.Node) root).getScene().getWindow();
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
                stage.setScene(scene);
            }
        } catch (Exception e) {
            System.err.println("[Profile] Failed to navigate back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ProfileScreen getScreen() {
        return screen;
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
