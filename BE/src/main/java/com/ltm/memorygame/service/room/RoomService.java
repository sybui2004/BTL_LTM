package com.ltm.memorygame.service.room;

import com.ltm.memorygame.dao.game.RoomRepository;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.mapper.RoomMapper;
import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserService userService;

    /*
    Tạo phòng chờ mới.
    - Host không được đang ở phòng khác.
    - Guest (nếu có) cũng không được ở phòng khác.
    */
    @Transactional
    public RoomResponseDTO createRoom(Long hostId, Long guestId) {
        User host = userService.getEntityById(hostId);
        User guest = guestId != null ? userService.getEntityById(guestId) : null;

        boolean hostInRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                hostId, hostId, List.of(RoomStatus.WAITING, RoomStatus.PLAYING, RoomStatus.READY)
        );

        if (hostInRoom) {
            throw new IllegalStateException("Host is already in a room!");
        }

        if (guest != null) {
            boolean guestInRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                    guestId, guestId, List.of(RoomStatus.WAITING, RoomStatus.PLAYING, RoomStatus.READY)
            );
            if (guestInRoom) {
                throw new IllegalStateException("Guest is already in a room!");
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
    Người chơi tham gia phòng.
    - Phòng phải đang WAITING.
    - Không cho host tự join phòng của chính mình.
    - Người chơi không được ở phòng khác.
    */
    @Transactional
    public RoomResponseDTO joinRoom(Long roomId, Long playerId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room is not available for joining!");
        }

        if (room.getHost().getId().equals(playerId)) {
            throw new IllegalStateException("Host cannot join their own room!");
        }

        if (room.getGuest() != null) {
            throw new IllegalStateException("Room already has a guest!");
        }

        boolean playerInOtherRoom = roomRepository.existsByHostIdOrGuestIdAndStatusIn(
                playerId, playerId, List.of(RoomStatus.WAITING, RoomStatus.READY, RoomStatus.PLAYING)
        );

        if (playerInOtherRoom) {
            throw new IllegalStateException("Player is already in another room!");
        }

        User guest = userService.getEntityById(playerId);
        room.setGuest(guest);
        room.setStatus(RoomStatus.READY);
        return RoomMapper.toDTO(roomRepository.save(room));
    }

    /*
    Người chơi thoát phòng.
    - Nếu host thoát, nếu phòng có 2 người thì chuyển host cho guest còn không thì xóa phòng.
    - Nếu guest thoát, phòng quay về trạng thái WAITING.
    */
    @Transactional
    public RoomResponseDTO exitRoom(Long roomId, Long playerId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        if (room.getHost().getId().equals(playerId)) {
            // Host thoát
            if (room.getGuest() == null) {
                // Không có guest -> xóa phòng
                roomRepository.delete(room);
                // Trả về DTO rỗng để BE/FE biết là phòng đã bị xóa
                room.setStatus(RoomStatus.DELETED);
                return RoomMapper.toDTO(room);
            } else {
                // Có guest -> chuyển quyền host cho guest
                User newHost = room.getGuest();
                room.setHost(newHost);
                room.setGuest(null);
                room.setStatus(RoomStatus.WAITING);
                return RoomMapper.toDTO(roomRepository.save(room));
            }
        }

        if (room.getGuest() != null && room.getGuest().getId().equals(playerId)) {
            // Guest thoát
            room.setGuest(null);
            room.setStatus(RoomStatus.WAITING);
            return RoomMapper.toDTO(roomRepository.save(room));
        }

        throw new IllegalStateException("Player not found in this room!");
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        return RoomMapper.toDTO(room);
    }

    @Transactional
    public RoomResponseDTO startMatch(Long roomId, Long playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        if (room.getStatus() != RoomStatus.READY)
            throw new IllegalStateException("Room is not ready to start!");

        if (!room.getHost().getId().equals(playerId))
            throw new IllegalStateException("Only host can start the match!");

        room.setStatus(RoomStatus.PLAYING);
        roomRepository.save(room);

        // TODO: Tạo Match entity sau (MatchService.createMatch(room))
        // matchService.createMatch(room.getId(), room.getHost().getId(), room.getGuest().getId());

        return RoomMapper.toDTO(room);
    }
}
