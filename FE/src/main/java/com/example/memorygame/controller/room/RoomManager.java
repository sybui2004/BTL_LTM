package com.example.memorygame.controller.room;

import com.example.memorygame.model.game.InviteDTO;
import com.example.memorygame.model.game.RoomResponseDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.InviteApi;
import com.example.memorygame.utils.RoomApi;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Manages room operations (create, invite, accept/reject invites)
 */
public class RoomManager {
    private final RoomStateManager stateManager;
    private final BiConsumer<String, Alert.AlertType> alertHandler;
    private Runnable onLoadGuestInfo;
    
    public RoomManager(RoomStateManager stateManager, BiConsumer<String, Alert.AlertType> alertHandler) {
        this.stateManager = stateManager;
        this.alertHandler = alertHandler;
    }
    
    public void setOnLoadGuestInfo(Runnable callback) {
        this.onLoadGuestInfo = callback;
    }
    
    private Runnable onLoadHostInfo;
    
    public void setOnLoadHostInfo(Runnable callback) {
        this.onLoadHostInfo = callback;
    }
    
    /**
     * Create room when entering RoomScreen or use existing room
     */
    public void createRoom() {
        new Thread(() -> {
            try {
                UserSummary currentUser = UserApi.getCurrentUser();
                if (currentUser == null) {
                    System.err.println("[Room] Cannot get current user");
                    return;
                }
                
                // Check if user already has a waiting room (as host OR guest)
                List<RoomResponseDTO> waitingRooms = RoomApi.getWaitingRooms();
                for (RoomResponseDTO existingRoom : waitingRooms) {
                    boolean isHost = existingRoom.hostId != null && existingRoom.hostId.equals(currentUser.id);
                    boolean isGuest = existingRoom.guestId != null && existingRoom.guestId.equals(currentUser.id);
                    
                    if (isHost || isGuest) {
                        stateManager.setCurrentRoomId(existingRoom.id);
                        stateManager.setHost(isHost);
                        
                        // Load opponent info
                        if (isHost && existingRoom.guestId != null) {
                            stateManager.setCurrentGuestId(existingRoom.guestId);
                            System.out.println("[Room] Using existing room ID: " + existingRoom.id + " as host with guest: " + existingRoom.guestId);
                            Platform.runLater(() -> {
                                if (onLoadGuestInfo != null) {
                                    onLoadGuestInfo.run();
                                }
                            });
                        } else if (isGuest && existingRoom.hostId != null) {
                            stateManager.setCurrentHostId(existingRoom.hostId);
                            System.out.println("[Room] Using existing room ID: " + existingRoom.id + " as guest with host: " + existingRoom.hostId);
                            Platform.runLater(() -> {
                                if (onLoadHostInfo != null) {
                                    onLoadHostInfo.run();
                                }
                            });
                        } else {
                            System.out.println("[Room] Using existing room ID: " + existingRoom.id);
                        }
                        return;
                    }
                }
                
                // No existing room, create new one
                RoomResponseDTO room = RoomApi.createRoom(currentUser.id, null);
                if (room == null) {
                    System.err.println("[Room] Failed to create room");
                    return;
                }
                
                stateManager.setCurrentRoomId(room.id);
                System.out.println("[Room] Created new room ID: " + room.id);
            } catch (Exception e) {
                System.err.println("[Room] Error creating room: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Handle invite user button click
     */
    public void handleInviteUser(Long targetUserId) {
        if (stateManager.getCurrentRoomId() == null) {
            Platform.runLater(() -> alertHandler.accept("Room is not ready. Please wait!", Alert.AlertType.WARNING));
            return;
        }
        
        new Thread(() -> {
            try {
                UserSummary currentUser = UserApi.getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> alertHandler.accept("Cannot get current user information!", Alert.AlertType.ERROR));
                    return;
                }
                
                boolean success = RoomApi.sendInvite(stateManager.getCurrentRoomId(), currentUser.id, targetUserId);
                
                Platform.runLater(() -> {
                    if (success) {
                        alertHandler.accept("Invite sent successfully!", Alert.AlertType.INFORMATION);
                    } else {
                        alertHandler.accept("Cannot send invite. Please try again!", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> alertHandler.accept("Error: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }
    
    /**
     * Load pending invites for current user
     */
    public void loadInvites(InviteListCallback callback) {
        new Thread(() -> {
            try {
                UserSummary currentUser = UserApi.getCurrentUser();
                if (currentUser == null) {
                    System.err.println("[Invite] Cannot get current user");
                    return;
                }
                
                List<InviteDTO> invites = InviteApi.getPendingInvites(currentUser.id);
                Platform.runLater(() -> callback.onInvitesLoaded(invites));
            } catch (Exception e) {
                System.err.println("[Invite] Error loading invites: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Handle accept invite
     */
    public void handleAcceptInvite(InviteDTO invite, Runnable onSuccess) {
        new Thread(() -> {
            try {
                UserSummary currentUser = UserApi.getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> alertHandler.accept("Cannot get current user information!", Alert.AlertType.ERROR));
                    return;
                }
                
                boolean success = InviteApi.acceptInvite(invite.roomId, currentUser.id);
                
                Platform.runLater(() -> {
                    if (success) {
                        alertHandler.accept("Invite accepted successfully!", Alert.AlertType.INFORMATION);
                        onSuccess.run();
                    } else {
                        alertHandler.accept("Cannot accept invite. Please try again!", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> alertHandler.accept("Error: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }
    
    /**
     * Handle reject invite
     */
    public void handleRejectInvite(InviteDTO invite, Runnable onSuccess) {
        new Thread(() -> {
            try {
                UserSummary currentUser = UserApi.getCurrentUser();
                if (currentUser == null) {
                    Platform.runLater(() -> alertHandler.accept("Cannot get current user information!", Alert.AlertType.ERROR));
                    return;
                }
                
                boolean success = InviteApi.rejectInvite(invite.roomId, currentUser.id);
                
                Platform.runLater(() -> {
                    if (success) {
                        alertHandler.accept("Invite rejected successfully!", Alert.AlertType.INFORMATION);
                        onSuccess.run();
                    } else {
                        alertHandler.accept("Cannot reject invite. Please try again!", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> alertHandler.accept("Error: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }
    
    @FunctionalInterface
    public interface InviteListCallback {
        void onInvitesLoaded(List<InviteDTO> invites);
    }
}

