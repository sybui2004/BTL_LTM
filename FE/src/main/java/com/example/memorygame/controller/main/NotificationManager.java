package com.example.memorygame.controller.main;

import com.example.memorygame.utils.FriendApi;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Manages friend request notifications
 */
public class NotificationManager {
    private final VBox notificationsContainer;
    private final Circle notificationBadge;
    private final Function<String, Image> avatarLoader;

    public NotificationManager(VBox notificationsContainer, Circle notificationBadge,
            Function<String, Image> avatarLoader) {
        this.notificationsContainer = notificationsContainer;
        this.notificationBadge = notificationBadge;
        this.avatarLoader = avatarLoader;
    }

    public void loadFriendRequests() {
        new Thread(() -> {
            try {
                List<Map<String, Object>> requests = FriendApi.getPendingRequests();
                Platform.runLater(() -> {
                    updateNotificationBadge(requests.size());
                    displayNotifications(requests);
                });
            } catch (Exception e) {
                System.err.println("[Notifications] Failed to load friend requests: " + e.getMessage());
            }
        }).start();
    }

    public void refreshNotifications() {
        loadFriendRequests();
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge != null) {
            notificationBadge.setVisible(count > 0);
        }
    }

    private void displayNotifications(List<Map<String, Object>> requests) {
        if (notificationsContainer == null)
            return;

        notificationsContainer.getChildren().clear();

        if (requests.isEmpty()) {
            Label emptyLabel = new Label("No new notifications");
            emptyLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-padding: 20;");
            notificationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> request : requests) {
            notificationsContainer.getChildren().add(createNotificationItem(request));
        }
    }

    private VBox createNotificationItem(Map<String, Object> request) {
        VBox item = new VBox(10);
        item.getStyleClass().addAll("notification-item", "unread");

        // Get request info - API returns FriendDTO directly (not nested fromUser)
        // friendRecordId: ID của Friend record (dùng để accept/reject)
        // id: ID của user gửi request
        // displayName, avatarUrl, status: thông tin user
        Object friendRecordIdObj = request.get("friendRecordId");
        Object idObj = request.get("id");
        
        // Calculate friendRecordId: use friendRecordId if available, otherwise use id
        final Long friendRecordId = friendRecordIdObj != null 
            ? ((Number) friendRecordIdObj).longValue() 
            : (idObj != null ? ((Number) idObj).longValue() : null);
        
        String fromDisplayName = request.get("displayName") != null ? request.get("displayName").toString() : "Unknown";
        String avatarUrl = request.get("avatarUrl") != null ? request.get("avatarUrl").toString() : null;

        // Header with avatar and name
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView avatar = new ImageView(avatarLoader.apply(avatarUrl));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);

        VBox info = new VBox(3);
        Label titleLabel = new Label("Friend Request");
        titleLabel.getStyleClass().add("notification-title");

        Label messageLabel = new Label(fromDisplayName + " sent you a friend request");
        messageLabel.getStyleClass().add("notification-message");

        info.getChildren().addAll(titleLabel, messageLabel);
        header.getChildren().addAll(avatar, info);

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("notification-actions");

        Button acceptBtn = new Button("✓");
        acceptBtn.getStyleClass().add("accept-btn-icon");
        acceptBtn.setTooltip(new javafx.scene.control.Tooltip("Accept"));
        acceptBtn.setOnAction(e -> handleAcceptRequest(friendRecordId, item));

        Button rejectBtn = new Button("✗");
        rejectBtn.getStyleClass().add("reject-btn-icon");
        rejectBtn.setTooltip(new javafx.scene.control.Tooltip("Reject"));
        rejectBtn.setOnAction(e -> handleRejectRequest(friendRecordId, item));

        actions.getChildren().addAll(acceptBtn, rejectBtn);

        item.getChildren().addAll(header, actions);
        return item;
    }

    private void handleAcceptRequest(Long requestId, VBox item) {
        if (requestId == null)
            return;

        // Disable buttons to prevent double-click
        item.getChildren().forEach(child -> {
            if (child instanceof HBox) {
                ((HBox) child).getChildren().forEach(btn -> {
                    if (btn instanceof Button) {
                        btn.setDisable(true);
                    }
                });
            }
        });

        new Thread(() -> {
            try {
                boolean success = FriendApi.acceptFriendRequest(requestId);
                Platform.runLater(() -> {
                    if (success) {
                        notificationsContainer.getChildren().remove(item);
                        // Recalculate badge count after removal
                        int remainingCount = notificationsContainer.getChildren().size();
                        updateNotificationBadge(remainingCount);
                    } else {
                        // Re-enable buttons on failure
                        item.getChildren().forEach(child -> {
                            if (child instanceof HBox) {
                                ((HBox) child).getChildren().forEach(btn -> {
                                    if (btn instanceof Button) {
                                        btn.setDisable(false);
                                    }
                                });
                            }
                        });
                    }
                });
            } catch (Exception e) {
                System.err.println("[Notifications] Failed to accept friend request: " + e.getMessage());
                Platform.runLater(() -> {
                    // Re-enable buttons on error
                    item.getChildren().forEach(child -> {
                        if (child instanceof HBox) {
                            ((HBox) child).getChildren().forEach(btn -> {
                                if (btn instanceof Button) {
                                    btn.setDisable(false);
                                }
                            });
                        }
                    });
                });
            }
        }).start();
    }

    private void handleRejectRequest(Long requestId, VBox item) {
        if (requestId == null)
            return;

        // Disable buttons to prevent double-click
        item.getChildren().forEach(child -> {
            if (child instanceof HBox) {
                ((HBox) child).getChildren().forEach(btn -> {
                    if (btn instanceof Button) {
                        btn.setDisable(true);
                    }
                });
            }
        });

        new Thread(() -> {
            try {
                boolean success = FriendApi.rejectFriendRequest(requestId);
                Platform.runLater(() -> {
                    if (success) {
                        notificationsContainer.getChildren().remove(item);
                        // Recalculate badge count after removal
                        int remainingCount = notificationsContainer.getChildren().size();
                        updateNotificationBadge(remainingCount);
                    } else {
                        // Re-enable buttons on failure
                        item.getChildren().forEach(child -> {
                            if (child instanceof HBox) {
                                ((HBox) child).getChildren().forEach(btn -> {
                                    if (btn instanceof Button) {
                                        btn.setDisable(false);
                                    }
                                });
                            }
                        });
                    }
                });
            } catch (Exception e) {
                System.err.println("[Notifications] Failed to reject friend request: " + e.getMessage());
                Platform.runLater(() -> {
                    // Re-enable buttons on error
                    item.getChildren().forEach(child -> {
                        if (child instanceof HBox) {
                            ((HBox) child).getChildren().forEach(btn -> {
                                if (btn instanceof Button) {
                                    btn.setDisable(false);
                                }
                            });
                        }
                    });
                });
            }
        }).start();
    }
}
