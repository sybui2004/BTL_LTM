package com.ltm.memorygame.service.room;

import com.ltm.memorygame.dao.game.RoomRepository;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.event.RoomGuestLeftEvent;
import com.ltm.memorygame.mapper.RoomMapper;
import com.ltm.memorygame.dto.game.response.MatchResponseDTO;
import com.ltm.memorygame.dto.game.request.CreateMatchRequest;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InviteService inviteService;

    /*
    Create a new waiting room.
    - Host cannot be in another room.
    - Guest (if any) cannot be in another room.
    */
    @Transactional
    public RoomResponseDTO createRoom(Long hostId, Long guestId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + hostId));
        User guest = guestId != null ? userRepository.findById(guestId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + guestId)) : null;

        boolean hostInRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                hostId, hostId, List.of(RoomStatus.WAITING, RoomStatus.PLAYING, RoomStatus.READY)
        );

        if (hostInRoom) {
            throw new IllegalStateException("Host is already in another room");
        }

        if (guest != null) {
            boolean guestInRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                    guestId, guestId, List.of(RoomStatus.WAITING, RoomStatus.PLAYING, RoomStatus.READY)
            );
            if (guestInRoom) {
                throw new IllegalStateException("Guest is already in another room");
            }
        }

        Room room = new Room();
        room.setHost(host);
        room.setGuest(guest);
        if (guest != null) {
            room.setStatus(RoomStatus.READY);
        } else {
            room.setStatus(RoomStatus.WAITING);
        }


        return RoomMapper.toDTO(roomRepository.save(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getWaitingRooms() {
        return RoomMapper.toDTOList(roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.WAITING));
    }

    /*
    Player joins the room.
    - Room must be in WAITING status.
    - Cannot allow host to join their own room.
    - Player cannot be in another room.
    */
    @Transactional
    public RoomResponseDTO joinRoom(Long roomId, Long playerId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room is not available for joining");
        }

        if (room.getHost().getId().equals(playerId)) {
            throw new IllegalStateException("Host cannot join their own room");
        }

        if (room.getGuest() != null) {
            throw new IllegalStateException("Room already has a guest");
        }

        boolean playerInOtherRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                playerId, playerId, List.of(RoomStatus.WAITING, RoomStatus.READY, RoomStatus.PLAYING)
        );

        if (playerInOtherRoom) {
            throw new IllegalStateException("Player is already in another room");
        }

        User guest = userRepository.findById(playerId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + playerId));
        room.setGuest(guest);
        room.setStatus(RoomStatus.READY);
        return RoomMapper.toDTO(roomRepository.save(room));
    }

    /*
    Player exits the room.
    - If host exits, if the room has 2 players, transfer host to guest, otherwise delete the room.
    - If guest exits, the room returns to the WAITING status.
    */
    @Transactional
    public RoomResponseDTO exitRoom(Long roomId, Long playerId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        if (room.getHost().getId().equals(playerId)) {
            // Host exits
            if (room.getGuest() == null) {
                // No guest -> delete invites first, then delete the room
                try {
                    inviteService.deleteAllByRoomId(roomId);
                } catch (Exception e) {
                    // Log but continue with the room deletion
                    System.err.println("[RoomService] Error deleting invites for room " + roomId + ": " + e.getMessage());
                }
                
                roomRepository.delete(room);
                // Return empty DTO to BE/FE to know that the room has been deleted
                room.setStatus(RoomStatus.DELETED);
                return RoomMapper.toDTO(room);
            } else {
                // There is a guest -> transfer host to guest
                User newHost = room.getGuest();
                room.setHost(newHost);
                room.setGuest(null);
                room.setStatus(RoomStatus.WAITING);
                RoomResponseDTO result = RoomMapper.toDTO(roomRepository.save(room));
                
                // Publish event to notify the new host via TCP
                try {
                    eventPublisher.publishEvent(new com.ltm.memorygame.event.HostPromotedEvent(this, room.getId(), newHost.getUsername()));
                } catch (Exception e) {
                    // Ignore event publish errors
                }
                
                return result;
            }
        }

        if (room.getGuest() != null && room.getGuest().getId().equals(playerId)) {
            // Guest thoát
            User host = room.getHost();
            room.setGuest(null);
            room.setStatus(RoomStatus.WAITING);
            RoomResponseDTO result = RoomMapper.toDTO(roomRepository.save(room));
            
            // Publish event to notify host via TCP
            try {
                eventPublisher.publishEvent(new RoomGuestLeftEvent(this, room.getId(), host.getUsername()));
            } catch (Exception e) {
                // Ignore event publish errors
            }
            
            return result;
        }

        throw new IllegalStateException("Player not found in this room");
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        return RoomMapper.toDTO(room);
    }

    @Transactional(readOnly = true)
    public Room getEntityById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
    }

    @Transactional
    public RoomResponseDTO updateAndMap(Room room) {
        return RoomMapper.toDTO(roomRepository.save(room));
    }
    
    /**
     * Find all active rooms where player is host or guest
     */
    @Transactional(readOnly = true)
    public List<Room> findRoomsByPlayer(Long playerId) {
        return roomRepository.findByHostIdOrGuestIdAndStatusIn(
            playerId, playerId, List.of(RoomStatus.WAITING, RoomStatus.READY, RoomStatus.PLAYING)
        );
    }

    // startMatch moved to RoomFacadeService to avoid service-to-service calls
}
