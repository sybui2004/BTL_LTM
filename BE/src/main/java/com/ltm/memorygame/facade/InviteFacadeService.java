package com.ltm.memorygame.facade;

import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.model.enums.InviteStatus;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.enums.NotificationTypeName;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.game.RoomInvite;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.notification.NotificationService;
import com.ltm.memorygame.service.room.InviteService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.tcp.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InviteFacadeService {

	private final InviteService inviteService;
	private final RoomService roomService;
	private final UserService userService;
	private final NotificationService notificationService;
	private final TCPServer tcpServer;

	@Transactional
	public void sendInvite(Long roomId, Long senderId, Long receiverId) {
		if (Objects.equals(senderId, receiverId)) {
			throw new IllegalStateException("You cannot invite yourself!");
		}

		boolean exists = inviteService.existsPendingInvite(roomId, receiverId);
		if (exists) {
			throw new IllegalStateException("You already invited this player for this room!");
		}

		Room room = roomService.getEntityById(roomId);
		if (!Objects.equals(room.getHost().getId(), senderId)) {
			throw new IllegalStateException("Only the host can send an invite!");
		}
		
		// Check if receiver is already in the same room
		if (room.getGuest() != null && Objects.equals(room.getGuest().getId(), receiverId)) {
			throw new IllegalStateException("This player is already in your room!");
		}

		User sender = userService.getEntityById(senderId);
		User receiver = userService.getEntityById(receiverId);

		RoomInvite invite = new RoomInvite();
		invite.setRoom(room);
		invite.setSender(sender);
		invite.setReceiver(receiver);
		invite.setStatus(InviteStatus.PENDING);
		inviteService.save(invite);

        try {
            notificationService.sendNotification(senderId, receiverId,
                    NotificationTypeName.MATCH_INVITE_RECEIVED.name(),
                    "You received a match invite from " + sender.getUsername());
        } catch (Exception ignored) {}
        
        // Send real-time TCP notification to receiver if online
        try {
            tcpServer.sendInviteNotification(receiver.getUsername(), roomId, sender.getUsername());
        } catch (Exception e) {
            // Ignore if TCP fails (user might be offline)
        }
	}

	@Transactional
	public RoomResponseDTO acceptInvite(Long roomId, Long receiverId) {
		RoomInvite invite = inviteService.findPendingByRoomAndReceiver(roomId, receiverId)
				.orElseThrow(() -> new NoSuchElementException("No pending invite found!"));

		// Exit from current room if player is in another room
		try {
			java.util.List<Room> currentRooms = roomService.findRoomsByPlayer(receiverId);
			for (Room currentRoom : currentRooms) {
				if (!currentRoom.getId().equals(roomId)) {
					roomService.exitRoom(currentRoom.getId(), receiverId);
					System.out.println("[Invite] User " + receiverId + " exited room " + currentRoom.getId());
				}
			}
		} catch (Exception e) {
			System.err.println("[Invite] Error exiting previous room: " + e.getMessage());
		}

		invite.setStatus(InviteStatus.ACCEPTED);
		inviteService.save(invite);

		Room room = invite.getRoom();
		room.setGuest(invite.getReceiver());
		room.setStatus(RoomStatus.READY);
		RoomResponseDTO result = roomService.updateAndMap(room);
		
		User host = room.getHost();
		User guest = room.getGuest();
		
		// Notify host via TCP that guest has joined
		try {
			tcpServer.sendRoomUpdateNotification(
				host.getUsername(), 
				roomId,
				guest.getId(),
				guest.getDisplayName(), 
				guest.getAvatarUrl()
			);
		} catch (Exception e) {
			System.err.println("[Invite] Error sending room update to host: " + e.getMessage());
		}
		
		// Notify guest via TCP with full room info (so guest can update their own UI)
		try {
			tcpServer.sendRoomJoinedNotification(
				guest.getUsername(), 
				roomId,
				host.getId(),
				host.getDisplayName(), 
				host.getAvatarUrl()
			);
		} catch (Exception e) {
			System.err.println("[Invite] Error sending room joined to guest: " + e.getMessage());
		}
		
		return result;
	}

	@Transactional
	public void rejectInvite(Long roomId, Long receiverId) {
		RoomInvite invite = inviteService.findPendingByRoomAndReceiver(roomId, receiverId)
				.orElseThrow(() -> new NoSuchElementException("No pending invite found!"));
		invite.setStatus(InviteStatus.REJECTED);
		inviteService.save(invite);
	}

	@Transactional(readOnly = true)
	public List<RoomInviteResponseDTO> getPendingInvites(Long receiverId) {
		return inviteService.getPendingInvites(receiverId);
	}
}
