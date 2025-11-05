package com.example.memorygame.controller.room;

import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;
import com.example.memorygame.utils.RoomApi;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.Map;

/**
 * Handles all TCP message listeners for RoomScreen
 */
public class TCPMessageHandler {
    private final RoomUIUpdater uiUpdater;
    private final Runnable onRefreshTab;
    private final Runnable onLoadInvites;
    private final RoomStateManager stateManager;

    public TCPMessageHandler(RoomUIUpdater uiUpdater,
            Runnable onRefreshTab,
            Runnable onLoadInvites,
            RoomStateManager stateManager) {
        this.uiUpdater = uiUpdater;
        this.onRefreshTab = onRefreshTab;
        this.onLoadInvites = onLoadInvites;
        this.stateManager = stateManager;
    }

    public void setupListeners() {
        TCPClient client = TCPClient.getInstance();

        setupUserStatusHandler(client);
        setupInviteReceivedHandler(client);
        setupRoomUpdatedHandler(client);
        setupRoomJoinedHandler(client);
        setupGuestLeftHandler(client);
        setupHostPromotedHandler(client);
        setupProfileUpdatedHandler(client);
    }

    private void setupUserStatusHandler(TCPClient client) {
        client.onMessage("USER_STATUS", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object userObj = data.get("user");
                Object onlineObj = data.get("online");

                if (userObj != null && onlineObj != null) {
                    String username = userObj.toString();
                    boolean online = Boolean.parseBoolean(onlineObj.toString());

                    System.out.println("[TCP][RoomScreen] User status changed: " + username +
                            " -> " + (online ? "ONLINE" : "OFFLINE"));

                    Platform.runLater(onRefreshTab);
                }
            }
        });
    }

    private void setupInviteReceivedHandler(TCPClient client) {
        client.onMessage("INVITE_RECEIVED", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object senderNameObj = data.get("senderName");
                if (senderNameObj != null) {
                    String senderName = senderNameObj.toString();
                    System.out.println("[TCP][RoomScreen] Received invite from: " + senderName);
                    Platform.runLater(onLoadInvites);
                }
            }
        });
    }

    private void setupRoomUpdatedHandler(TCPClient client) {
        client.onMessage("ROOM_UPDATED", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object guestIdObj = data.get("guestId");
                Object guestDisplayNameObj = data.get("guestDisplayName");
                Object guestAvatarUrlObj = data.get("guestAvatarUrl");

                if (guestDisplayNameObj != null) {
                    String guestDisplayName = guestDisplayNameObj.toString();
                    String guestAvatarUrl = guestAvatarUrlObj != null ? guestAvatarUrlObj.toString() : null;

                    // Store guest ID
                    if (guestIdObj != null) {
                        try {
                            Long guestId = Long.parseLong(guestIdObj.toString());
                            stateManager.setCurrentGuestId(guestId);
                            System.out.println("[TCP][RoomScreen] Guest ID: " + guestId + " joined room");
                        } catch (NumberFormatException e) {
                            stateManager.setCurrentGuestId(null);
                        }
                    }

                    System.out.println("[TCP][RoomScreen] Guest joined room: " + guestDisplayName);

                    Platform.runLater(() -> {
                        uiUpdater.updateGuestInfo(guestDisplayName, guestAvatarUrl);
                        onRefreshTab.run();
                        showAlert(guestDisplayName + " joined the room!");
                    });
                }
            }
        });
    }

    private void setupRoomJoinedHandler(TCPClient client) {
        client.onMessage("ROOM_JOINED", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object roomIdObj = data.get("roomId");
                Object hostIdObj = data.get("hostId");
                Object hostDisplayNameObj = data.get("hostDisplayName");
                Object hostAvatarUrlObj = data.get("hostAvatarUrl");

                if (hostDisplayNameObj != null) {
                    String hostDisplayName = hostDisplayNameObj.toString();
                    String hostAvatarUrl = hostAvatarUrlObj != null ? hostAvatarUrlObj.toString() : null;

                    // Store room ID (IMPORTANT: update to new room!)
                    if (roomIdObj != null) {
                        try {
                            Long roomId = Long.parseLong(roomIdObj.toString());
                            stateManager.setCurrentRoomId(roomId);
                            System.out.println("[TCP][RoomScreen] Joined room ID: " + roomId);
                        } catch (NumberFormatException e) {
                            System.err.println("[TCP][RoomScreen] Invalid room ID: " + roomIdObj);
                        }
                    }

                    // Store host ID
                    if (hostIdObj != null) {
                        try {
                            Long hostId = Long.parseLong(hostIdObj.toString());
                            stateManager.setCurrentHostId(hostId);
                            System.out.println("[TCP][RoomScreen] Host ID: " + hostId);
                        } catch (NumberFormatException e) {
                            stateManager.setCurrentHostId(null);
                        }
                    }

                    System.out.println("[TCP][RoomScreen] Joined room with host: " + hostDisplayName);

                    Platform.runLater(() -> {
                        uiUpdater.updateHostInfo(hostDisplayName, hostAvatarUrl);

                        // Show myself as guest
                        UserSummary currentUser = UserApi.getCurrentUser();
                        if (currentUser != null) {
                            uiUpdater.updateGuestInfo(currentUser.displayName, currentUser.avatarUrl);
                        }

                        // I am guest, disable play button
                        stateManager.setHost(false);
                        uiUpdater.setPlayButtonEnabled(false);

                        onRefreshTab.run();
                    });
                }
            }
        });
    }

    private void setupGuestLeftHandler(TCPClient client) {
        client.onMessage("GUEST_LEFT", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                System.out.println("[TCP][RoomScreen] Guest left the room");

                Platform.runLater(() -> {
                    uiUpdater.clearGuestInfo();
                    stateManager.setCurrentGuestId(null);
                    stateManager.setCurrentHostId(null);
                    onRefreshTab.run();
                });
            }
        });
    }

    private void setupHostPromotedHandler(TCPClient client) {
        client.onMessage("HOST_PROMOTED", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                System.out.println("[TCP][RoomScreen] You have been promoted to host!");

                Platform.runLater(() -> {
                    stateManager.setHost(true);
                    uiUpdater.setPlayButtonEnabled(true);

                    UserSummary currentUser = UserApi.getCurrentUser();
                    if (currentUser != null) {
                        uiUpdater.updateHostInfo(currentUser.displayName, currentUser.avatarUrl);
                    }

                    uiUpdater.clearGuestInfo();
                    stateManager.setCurrentGuestId(null);
                    stateManager.setCurrentHostId(null);

                    onRefreshTab.run();
                    showAlert("You have been promoted to host of the room!");
                });
            }
        });
    }

    private void setupProfileUpdatedHandler(TCPClient client) {
        client.onMessage("USER_PROFILE_UPDATED", message -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object usernameObj = data.get("username");
                if (usernameObj != null) {
                    String updatedUsername = usernameObj.toString();
                    System.out.println("[TCP][RoomScreen] User profile updated: " + updatedUsername);

                    // Refresh friend list to show updated display names and avatars
                    Platform.runLater(() -> {
                        onRefreshTab.run();

                        // Check if the updated user is current user, host, or guest
                        new Thread(() -> {
                            try {
                                UserSummary currentUser = UserApi.getCurrentUser();
                                if (currentUser == null)
                                    return;

                                // Check if current user's profile was updated
                                if (currentUser.username != null && currentUser.username.equals(updatedUsername)) {
                                    // Reload current user to get updated info
                                    UserSummary updatedCurrentUser = UserApi.getCurrentUser();
                                    Platform.runLater(() -> {
                                        if (stateManager.isHost()) {
                                            uiUpdater.updateHostInfo(
                                                    updatedCurrentUser.displayName,
                                                    updatedCurrentUser.avatarUrl);
                                        } else {
                                            // Check if we are the guest
                                            Long guestId = stateManager.getCurrentGuestId();
                                            if (guestId != null && guestId.equals(updatedCurrentUser.id)) {
                                                uiUpdater.updateGuestInfo(
                                                        updatedCurrentUser.displayName,
                                                        updatedCurrentUser.avatarUrl);
                                            }
                                        }
                                    });
                                } else {
                                    // Updated user might be host or guest - reload room to get updated info
                                    Long roomId = stateManager.getCurrentRoomId();
                                    if (roomId != null) {
                                        try {
                                            com.example.memorygame.model.game.RoomResponseDTO room = RoomApi
                                                    .getRoom(roomId);
                                            if (room != null) {
                                                // Check if updated user is host
                                                if (room.hostId != null) {
                                                    UserSummary hostUser = UserApi.getUserById(room.hostId);
                                                    if (hostUser != null && hostUser.username != null
                                                            && hostUser.username.equals(updatedUsername)) {
                                                        Platform.runLater(() -> {
                                                            uiUpdater.updateHostInfo(
                                                                    hostUser.displayName,
                                                                    hostUser.avatarUrl);
                                                        });
                                                    }
                                                }
                                                // Check if updated user is guest
                                                if (room.guestId != null) {
                                                    UserSummary guestUser = UserApi.getUserById(room.guestId);
                                                    if (guestUser != null && guestUser.username != null
                                                            && guestUser.username.equals(updatedUsername)) {
                                                        Platform.runLater(() -> {
                                                            uiUpdater.updateGuestInfo(
                                                                    guestUser.displayName,
                                                                    guestUser.avatarUrl);
                                                        });
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            System.err.println(
                                                    "[TCP][RoomScreen] Error reloading room info: " + e.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("[TCP][RoomScreen] Error updating profile info: " + e.getMessage());
                            }
                        }).start();
                    });
                }
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
