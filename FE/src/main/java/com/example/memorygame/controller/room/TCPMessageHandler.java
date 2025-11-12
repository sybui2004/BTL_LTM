package com.example.memorygame.controller.room;

import java.util.Map;

import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.RoomApi;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Handles all TCP message listeners for RoomScreen
 */
public class TCPMessageHandler {
    private final RoomUIUpdater uiUpdater;
    private final Runnable onRefreshTab;
    private final Runnable onLoadInvites;
    private final RoomStateManager stateManager;
    private final Runnable onComboBoxStateUpdate;
    private final SettingsUpdateCallback onSettingsUpdate;
    private final Runnable onSendCurrentSettings;
    private final GameStartCallback onGameStart;
    private final Runnable onRoomJoined;
    
    @FunctionalInterface
    public interface SettingsUpdateCallback {
        void accept(String theme, String size, String time);
    }
    
    @FunctionalInterface
    public interface GameStartCallback {
        void accept(String theme, String size, String time, String player1Name, String player2Name, Long matchId);
    }
    
    public TCPMessageHandler(RoomUIUpdater uiUpdater, 
                            Runnable onRefreshTab,
                            Runnable onLoadInvites,
                            RoomStateManager stateManager,
                            Runnable onComboBoxStateUpdate,
                            SettingsUpdateCallback onSettingsUpdate,
                            Runnable onSendCurrentSettings,
                            GameStartCallback onGameStart,
                            Runnable onRoomJoined) {
        this.uiUpdater = uiUpdater;
        this.onRefreshTab = onRefreshTab;
        this.onLoadInvites = onLoadInvites;
        this.stateManager = stateManager;
        this.onComboBoxStateUpdate = onComboBoxStateUpdate;
        this.onSettingsUpdate = onSettingsUpdate;
        this.onSendCurrentSettings = onSendCurrentSettings;
        this.onGameStart = onGameStart;
        this.onRoomJoined = onRoomJoined;
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
            System.out.println("[TCP][RoomScreen] Received INVITE_RECEIVED message: " + message.getData());
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object senderNameObj = data.get("senderName");
                if (senderNameObj != null) {
                    String senderName = senderNameObj.toString();
                    System.out.println("[TCP][RoomScreen] Received invite from: " + senderName);
                    Platform.runLater(onLoadInvites);
                } else {
                    System.err.println("[TCP][RoomScreen] INVITE_RECEIVED message missing senderName");
                }
            } else {
                System.err.println("[TCP][RoomScreen] INVITE_RECEIVED message has no data");
            }
        });
    }

    private void setupRoomUpdatedHandler(TCPClient client) {
        client.onMessage("ROOM_UPDATED", message -> {
            System.out.println("[TCP][RoomScreen] Received ROOM_UPDATED message: " + message.getData());
            Map<String, Object> data = message.getData();
            if (data != null) {
                Object guestIdObj = data.get("guestId");
                Object guestDisplayNameObj = data.get("guestDisplayName");
                Object guestAvatarUrlObj = data.get("guestAvatarUrl");
                
                System.out.println("[TCP][RoomScreen] Parsing ROOM_UPDATED - guestId: " + guestIdObj + ", displayName: " + guestDisplayNameObj);
                
                if (guestDisplayNameObj != null) {
                    String guestDisplayName = guestDisplayNameObj.toString();
                    String guestAvatarUrl = guestAvatarUrlObj != null ? guestAvatarUrlObj.toString() : null;

                    // Store guest ID
                    if (guestIdObj != null) {
                        try {
                            Long guestId = Long.parseLong(guestIdObj.toString());
                            stateManager.setCurrentGuestId(guestId);
                            System.out.println("[TCP][RoomScreen] Set guest ID: " + guestId);
                        } catch (NumberFormatException e) {
                            System.err.println("[TCP][RoomScreen] Failed to parse guestId: " + guestIdObj);
                            stateManager.setCurrentGuestId(null);
                        }
                    }

                    System.out.println("[TCP][RoomScreen] Guest joined room: " + guestDisplayName);

                    Platform.runLater(() -> {
                        System.out.println("[TCP][RoomScreen] Updating UI with guest info: " + guestDisplayName);
                        uiUpdater.updateGuestInfo(guestDisplayName, guestAvatarUrl);
                        
                        // Enable play button for host when guest joins
                        boolean canStart = stateManager.isHost() && stateManager.canStartGame();
                        System.out.println("[DEBUG] Guest joined - isHost: " + stateManager.isHost() + ", canStartGame: " + stateManager.canStartGame() + ", enabling play button: " + canStart);
                        uiUpdater.setPlayButtonEnabled(canStart);
                        
                        // Update ComboBox states
                        if (onComboBoxStateUpdate != null) {
                            onComboBoxStateUpdate.run();
                        }
                        
                        // Send current settings to the newly joined guest
                        if (onSendCurrentSettings != null) {
                            onSendCurrentSettings.run();
                        }
                        
                        // Refresh invite list to remove accepted invite
                        if (onLoadInvites != null) {
                            onLoadInvites.run();
                        }
                        
                        onRefreshTab.run();
                        showAlert(guestDisplayName + " joined the room!");
                    });
                } else {
                    System.err.println("[TCP][RoomScreen] ROOM_UPDATED message missing guestDisplayName");
                }
            } else {
                System.err.println("[TCP][RoomScreen] ROOM_UPDATED message has null data");
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
                            // Use displayName for UI, but set username for game settings
                            String myDisplayName = currentUser.displayName != null && !currentUser.displayName.isBlank() 
                                ? currentUser.displayName : currentUser.username;
                            uiUpdater.updateGuestInfo(myDisplayName, currentUser.avatarUrl);
                        }
                        
                        // Refresh invite list to remove accepted invite
                        if (onLoadInvites != null) {
                            onLoadInvites.run();
                        }

                        // I am guest, disable play button
                        stateManager.setHost(false);
                        uiUpdater.setPlayButtonEnabled(false);
                        
                        // Update ComboBox states
                        if (onComboBoxStateUpdate != null) {
                            onComboBoxStateUpdate.run();
                        }
                        
                        // Initialize lobby chat khi join room
                        if (onRoomJoined != null) {
                            onRoomJoined.run();
                        }
                        
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
                    
                    // Update play button state - enable if host, disable if guest
                    uiUpdater.setPlayButtonEnabled(stateManager.isHost() && stateManager.canStartGame());
                    
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
                    // Host has no guest yet -> disable Play until someone joins
                    uiUpdater.setPlayButtonEnabled(stateManager.isHost() && stateManager.canStartGame());

                    UserSummary currentUser = UserApi.getCurrentUser();
                    if (currentUser != null) {
                        uiUpdater.updateHostInfo(currentUser.displayName, currentUser.avatarUrl);
                        // Track the new host ID in state
                        stateManager.setCurrentHostId(currentUser.id);
                    }

                    uiUpdater.clearGuestInfo();
                    stateManager.setCurrentGuestId(null);
                    
                    // Update ComboBox states
                    if (onComboBoxStateUpdate != null) {
                        onComboBoxStateUpdate.run();
                    }
                    
                    onRefreshTab.run();
                    showAlert("You have been promoted to host of the room!");
                });
            }
        });
        
        // Handle room settings changes from host
        client.onMessage("ROOM_SETTINGS_CHANGED", message -> {
            System.out.println("[TCP][RoomScreen] Received ROOM_SETTINGS_CHANGED message");
            Map<String, Object> data = message.getData();
            if (data != null) {
                String theme = (String) data.get("theme");
                String size = (String) data.get("size");
                String time = (String) data.get("time");
                
                System.out.println("[TCP][RoomScreen] Received settings from host - Theme: " + theme + ", Size: " + size + ", Time: " + time);
                
                Platform.runLater(() -> {
                    // Update labels for guest
                    if (onSettingsUpdate != null) {
                        System.out.println("[TCP][RoomScreen] Calling onSettingsUpdate callback");
                        onSettingsUpdate.accept(theme, size, time);
                    } else {
                        System.out.println("[TCP][RoomScreen] onSettingsUpdate callback is null!");
                    }
                });
            } else {
                System.out.println("[TCP][RoomScreen] ROOM_SETTINGS_CHANGED message has no data!");
            }
        });
        
        // Handle game start from host
        client.onMessage("GAME_STARTED", message -> {
            System.out.println("[TCP][RoomScreen] Received GAME_STARTED message");
            Map<String, Object> data = message.getData();
            if (data != null) {
                String theme = (String) data.get("theme");
                String size = (String) data.get("size");
                String time = (String) data.get("time");
                String player1Name = (String) data.get("player1Name");
                String player2Name = (String) data.get("player2Name");
                Long matchId = null;
                Object matchIdObj = data.get("matchId");
                if (matchIdObj != null) {
                    try { matchId = Long.parseLong(matchIdObj.toString()); } catch (NumberFormatException ignored) {}
                }
                
                System.out.println("[TCP][RoomScreen] Game started by host - Theme: " + theme + ", Size: " + size + ", Time: " + time);
                System.out.println("[TCP][RoomScreen] Player names - Player1: " + player1Name + ", Player2: " + player2Name);
                
                final Long matchIdFinal = matchId;
                Platform.runLater(() -> {
                    // Start game for guest
                    if (onGameStart != null) {
                        onGameStart.accept(theme, size, time, player1Name, player2Name, matchIdFinal);
                    }
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
