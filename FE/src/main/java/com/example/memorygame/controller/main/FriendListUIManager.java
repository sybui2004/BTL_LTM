package com.example.memorygame.controller.main;

import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.FriendApi;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.example.memorygame.model.user.FriendListDTO;

/**
 * Manages friend list UI with tabs and search (RoomScreen style) for MainScreen
 */
public class FriendListUIManager {
    private final VBox listContainer;
    private final HBox searchContainer;
    private final TextField txtSearch;
    private final ToggleButton tabFriends;
    private final ToggleButton tabStrangers;
    private final Function<String, Image> avatarLoader;
    private final BiConsumer<Long, String> onViewProfile;
    private final BiConsumer<Long, String> onOpenChat;
    private final Map<String, HBox> friendItemsMap = new HashMap<>();
    private MainScreenFriendItemBuilder itemBuilder;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private enum Tab {
        FRIENDS, STRANGERS
    }

    private Tab currentTab = Tab.FRIENDS;
    private String lastSearchQuery = "";

    public FriendListUIManager(
            VBox listContainer,
            HBox searchContainer,
            TextField txtSearch,
            ToggleButton tabFriends,
            ToggleButton tabStrangers,
            Function<String, Image> avatarLoader,
            BiConsumer<Long, String> onViewProfile,
            BiConsumer<Long, String> onOpenChat) {
        this.listContainer = listContainer;
        this.searchContainer = searchContainer;
        this.txtSearch = txtSearch;
        this.tabFriends = tabFriends;
        this.tabStrangers = tabStrangers;
        this.avatarLoader = avatarLoader;
        this.onViewProfile = onViewProfile;
        this.onOpenChat = onOpenChat;
        initializeBuilder();
        setupTabs();
    }

    private void initializeBuilder() {
        this.itemBuilder = new MainScreenFriendItemBuilder(
                avatarLoader,
                this::handleViewProfile,
                this::handleSendFriendRequest,
                this::handleOpenChat);
    }

    public MainScreenFriendItemBuilder getFriendItemBuilder() {
        return itemBuilder;
    }

    private void setupTabs() {
        // Setup toggle group
        ToggleGroup group = new ToggleGroup();
        tabFriends.setToggleGroup(group);
        tabStrangers.setToggleGroup(group);

        // Tab listeners
        tabFriends.setOnAction(e -> switchTab(Tab.FRIENDS));
        tabStrangers.setOnAction(e -> switchTab(Tab.STRANGERS));

        // Enter to search
        if (txtSearch != null) {
            txtSearch.setOnAction(e -> handleSearch());
        }
    }

    private void switchTab(Tab tab) {
        currentTab = tab;

        // Show/hide search based on tab
        if (searchContainer != null) {
            searchContainer.setVisible(tab == Tab.STRANGERS);
            searchContainer.setManaged(tab == Tab.STRANGERS);
        }

        // Load data
        switch (tab) {
            case FRIENDS -> loadFriends();
            case STRANGERS -> {
                if (txtSearch != null) {
                    if (lastSearchQuery != null && !lastSearchQuery.isBlank()) {
                        txtSearch.setText(lastSearchQuery);
                    } else {
                        txtSearch.clear();
                    }
                }

                if (lastSearchQuery != null && !lastSearchQuery.isBlank()) {
                    performStrangerSearch(lastSearchQuery);
                } else {
                    showStrangerPrompt();
                }
            }
        }
    }

    public void selectFriendsTab() {
        switchTab(Tab.FRIENDS);
    }

    public void loadFriends() {
        new Thread(() -> {
            try {
                List<Map<String, Object>> friendsData = FriendApi.getFriends();
                // Convert to FriendDTO
                List<FriendDTO> friends = new ArrayList<>();
                for (Map<String, Object> data : friendsData) {
                    String json = MAPPER.writeValueAsString(data);
                    FriendDTO friend = MAPPER.readValue(json, FriendDTO.class);
                    friends.add(friend);
                }
                Platform.runLater(() -> displayFriends(friends));
            } catch (Exception e) {
                System.err.println("[FriendList] Failed to load friends: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public void handleSearch() {
        if (currentTab != Tab.STRANGERS) {
            return;
        }

        String query = txtSearch != null && txtSearch.getText() != null
                ? txtSearch.getText().trim()
                : "";

        performStrangerSearch(query);
    }

    private void displayFriends(List<FriendDTO> friends) {
        if (listContainer == null)
            return;

        listContainer.getChildren().clear();
        friendItemsMap.clear();

        if (friends.isEmpty()) {
            Label emptyLabel = new Label("No friends yet");
            emptyLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            listContainer.getChildren().add(emptyLabel);
            return;
        }

        for (FriendDTO friend : friends) {
            HBox item = itemBuilder.createFriendItem(friend);
            String username = friend.username;
            if (username != null) {
                friendItemsMap.put(username, item);
            }
            listContainer.getChildren().add(item);
        }
    }

    private void displayStrangers(List<FriendDTO> strangers, Set<Long> outgoingRequestIds) {
        if (listContainer == null)
            return;

        listContainer.getChildren().clear();

        if (strangers.isEmpty()) {
            Label emptyLabel = new Label("No users found");
            emptyLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
            listContainer.getChildren().add(emptyLabel);
            return;
        }

        for (FriendDTO stranger : strangers) {
            boolean hasOutgoingRequest = stranger.id != null && outgoingRequestIds.contains(stranger.id);
            HBox item = itemBuilder.createStrangerItem(stranger, hasOutgoingRequest);
            listContainer.getChildren().add(item);
        }
    }

    private void handleViewProfile(Long userId, String displayName) {
        Platform.runLater(() -> {
            if (onViewProfile != null) {
                onViewProfile.accept(userId, displayName);
            } else {
                // Fallback: open profile screen
                try {
                    com.example.memorygame.controller.ProfileScreenController profileController = new com.example.memorygame.controller.ProfileScreenController(
                            userId);
                    javafx.scene.Scene scene = new javafx.scene.Scene(profileController.getScreen().getRoot());
                    javafx.stage.Stage stage = (javafx.stage.Stage) listContainer.getScene().getWindow();

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
                    System.err.println("[FriendList] Failed to open profile: " + e.getMessage());
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi");
                    alert.setContentText("Không thể mở trang profile");
                    alert.showAndWait();
                }
            }
        });
    }

    private void handleOpenChat(Long userId, String displayName) {
        if (onOpenChat != null) {
            onOpenChat.accept(userId, displayName);
        }
    }

    private void handleSendFriendRequest(Long userId, String displayName) {
        new Thread(() -> {
            try {
                boolean success = sendFriendRequest(userId);
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("Friend request sent to " + displayName, Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Failed to send friend request", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                System.err.println("[FriendList] Failed to send friend request: " + e.getMessage());
                Platform.runLater(() -> showAlert("Failed to send friend request", Alert.AlertType.ERROR));
            }
        }).start();
    }

    private boolean sendFriendRequest(Long targetUserId) {
        var currentUser = UserApi.getCurrentUser();
        if (currentUser == null)
            return false;
        Long currentUserId = currentUser.id;
        if (currentUserId == null)
            return false;

        return FriendApi.sendFriendRequest(currentUserId, targetUserId);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleUserStatusChange(TCPClient.TCPMessage message) {
        Map<String, Object> data = message.getData();
        if (data == null)
            return;

        Object userObj = data.get("user");
        Object onlineObj = data.get("online");

        if (userObj == null || onlineObj == null)
            return;

        // Reload current tab to update status
        Platform.runLater(() -> switchTab(currentTab));
    }

    /**
     * Refresh the currently displayed tab (friend list or strangers list)
     */
    public void refreshCurrentTab() {
        Platform.runLater(() -> {
            switch (currentTab) {
                case FRIENDS -> loadFriends();
                case STRANGERS -> {
                    if (lastSearchQuery != null && !lastSearchQuery.isBlank()) {
                        performStrangerSearch(lastSearchQuery);
                    } else {
                        showStrangerPrompt();
                    }
                }
            }
        });
    }

    private void performStrangerSearch(String query) {
        if (query == null || query.isBlank()) {
            showStrangerPrompt();
            return;
        }

        lastSearchQuery = query;

        new Thread(() -> {
            try {
                // Parse ID từ query
                long userId;
                try {
                    userId = Long.parseLong(query.trim());
                } catch (NumberFormatException e) {
                    Platform.runLater(() -> {
                        if (listContainer != null) {
                            listContainer.getChildren().clear();
                            Label errorLabel = new Label("ID không hợp lệ");
                            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-style: italic;");
                            listContainer.getChildren().add(errorLabel);
                        }
                    });
                    return;
                }

                // Lấy thông tin user hiện tại để kiểm tra
                UserSummary currentUser = UserApi.getCurrentUser();
                Long currentUserId = (currentUser != null) ? currentUser.id : null;

                // Tìm user theo ID
                UserSummary user = UserApi.getUserById(userId);

                Platform.runLater(() -> {
                    if (listContainer == null)
                        return;

                    listContainer.getChildren().clear();

                    if (user == null) {
                        Label notFoundLabel = new Label("Không tìm thấy người dùng");
                        notFoundLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
                        listContainer.getChildren().add(notFoundLabel);
                    } else if (currentUserId != null && user.id == currentUserId) {
                        Label selfLabel = new Label("Không thể tìm kiếm chính mình");
                        selfLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
                        listContainer.getChildren().add(selfLabel);
                    } else {
                        // Kiểm tra relationship với user này
                        try {
                            FriendListDTO friendList = FriendApi.getFriendList();
                            boolean isFriend = false;
                            Set<Long> outgoingRequestIds = new HashSet<>();

                            if (friendList != null) {
                                // Check if already friends
                                if (friendList.friends != null) {
                                    for (FriendDTO friend : friendList.friends) {
                                        if (friend.id != null && friend.id.equals(user.id)) {
                                            isFriend = true;
                                            break;
                                        }
                                    }
                                }

                                // Get outgoing request IDs
                                if (friendList.outgoingRequest != null) {
                                    for (FriendDTO request : friendList.outgoingRequest) {
                                        if (request.id != null) {
                                            outgoingRequestIds.add(request.id);
                                        }
                                    }
                                }
                            }

                            // Convert UserSummary to FriendDTO
                            FriendDTO friendDTO = new FriendDTO();
                            friendDTO.id = user.id;
                            friendDTO.username = user.username;
                            friendDTO.displayName = user.displayName;
                            friendDTO.avatarUrl = user.avatarUrl;
                            friendDTO.status = user.status != null ? user.status.toString() : "OFFLINE";

                            // Nếu đã là bạn bè, hiển thị như friend item (không có nút thêm bạn)
                            if (isFriend) {
                                HBox item = itemBuilder.createFriendItem(friendDTO);
                                listContainer.getChildren().add(item);
                            } else {
                                // Nếu chưa là bạn, hiển thị như stranger item
                                boolean hasOutgoingRequest = outgoingRequestIds.contains(user.id);
                                HBox item = itemBuilder.createStrangerItem(friendDTO, hasOutgoingRequest);
                                listContainer.getChildren().add(item);
                            }
                        } catch (Exception e) {
                            System.err.println("[FriendListUIManager] Error checking relationship: " + e.getMessage());
                            // Fallback: hiển thị như stranger nếu không thể kiểm tra relationship
                            FriendDTO stranger = new FriendDTO();
                            stranger.id = user.id;
                            stranger.username = user.username;
                            stranger.displayName = user.displayName;
                            stranger.avatarUrl = user.avatarUrl;
                            stranger.status = user.status != null ? user.status.toString() : "OFFLINE";

                            // Try to get outgoing requests in fallback
                            Set<Long> outgoingRequestIds = new HashSet<>();
                            try {
                                FriendListDTO friendList = FriendApi.getFriendList();
                                if (friendList != null && friendList.outgoingRequest != null) {
                                    for (FriendDTO request : friendList.outgoingRequest) {
                                        if (request.id != null) {
                                            outgoingRequestIds.add(request.id);
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println(
                                        "[FriendListUIManager] Error getting outgoing requests: " + ex.getMessage());
                            }

                            boolean hasOutgoingRequest = outgoingRequestIds.contains(user.id);
                            HBox item = itemBuilder.createStrangerItem(stranger, hasOutgoingRequest);
                            listContainer.getChildren().add(item);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[FriendListUIManager] Error searching: " + e.getMessage());
                Platform.runLater(() -> {
                    if (listContainer != null) {
                        listContainer.getChildren().clear();
                        Label errorLabel = new Label("Lỗi khi tìm kiếm: " + e.getMessage());
                        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-style: italic;");
                        listContainer.getChildren().add(errorLabel);
                    }
                });
            }
        }).start();
    }

    private void showStrangerPrompt() {
        if (listContainer == null)
            return;

        listContainer.getChildren().clear();
        Label promptLabel = new Label("Nhập ID để tìm kiếm người dùng");
        promptLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic;");
        listContainer.getChildren().add(promptLabel);
    }

    private Set<Long> getOutgoingFriendRequestIds() {
        try {
            com.example.memorygame.model.user.FriendListDTO friendList = FriendApi.getFriendList();
            Set<Long> ids = new HashSet<>();
            if (friendList != null && friendList.outgoingRequest != null) {
                for (FriendDTO request : friendList.outgoingRequest) {
                    if (request.id != null) {
                        ids.add(request.id);
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            System.err.println("[FriendList] Failed to get outgoing requests: " + e.getMessage());
            return Collections.emptySet();
        }
    }

}
