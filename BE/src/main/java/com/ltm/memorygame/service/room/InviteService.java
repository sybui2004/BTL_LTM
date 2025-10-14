package com.ltm.memorygame.service.room;

import com.ltm.memorygame.dao.game.RoomInviteRepository;
import com.ltm.memorygame.dao.game.RoomRepository;
import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.mapper.RoomInviteMapper;
import com.ltm.memorygame.mapper.RoomMapper;
import com.ltm.memorygame.model.enums.InviteStatus;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.model.game.RoomInvite;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final RoomInviteRepository inviteRepository;
    private final RoomRepository roomRepository;
    private final UserService userService;

    @Transactional
    public void sendInvite(Long roomId, Long senderId, Long receiverId) {

        if (Objects.equals(senderId, receiverId)) {
            throw new IllegalStateException("You cannot invite yourself!");
        }

        boolean exists = inviteRepository.existsByRoomIdAndReceiverIdAndStatus(roomId, receiverId, InviteStatus.PENDING);
        if (exists) {
            throw new IllegalStateException("You already invited this player for this room!");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));

        if (!Objects.equals(room.getHost().getId(), senderId))
            throw new IllegalStateException("Only the host can send an invite!");

        User sender = userService.getEntityById(senderId);

        User receiver = userService.getEntityById(receiverId);

        RoomInvite invite = new RoomInvite();
        invite.setRoom(room);
        invite.setSender(sender);
        invite.setReceiver(receiver);
        invite.setStatus(InviteStatus.PENDING);
        // TODO:Gửi notification realtime
        inviteRepository.save(invite);
    }

    @Transactional
    public RoomResponseDTO acceptInvite(Long roomId, Long receiverId) {
        RoomInvite invite = inviteRepository.findByRoomIdAndReceiverIdAndStatus(roomId, receiverId, InviteStatus.PENDING)
                .orElseThrow(() -> new NoSuchElementException("No pending invite found!"));

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        Room room = invite.getRoom();
        room.setGuest(invite.getReceiver());
        room.setStatus(RoomStatus.READY);
        roomRepository.save(room);

        // TODO:Gửi notification realtime
        return RoomMapper.toDTO(room);
    }

    @Transactional
    public void rejectInvite(Long roomId, Long receiverId) {
        RoomInvite invite = inviteRepository.findByRoomIdAndReceiverIdAndStatus(roomId, receiverId, InviteStatus.PENDING)
                .orElseThrow(() -> new NoSuchElementException("No pending invite found!"));

        invite.setStatus(InviteStatus.REJECTED);
        // TODO:Gửi notification realtime
        inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public List<RoomInviteResponseDTO> getPendingInvites(Long receiverId) {
        List<RoomInvite> invites = inviteRepository.findByReceiverIdAndStatus(receiverId, InviteStatus.PENDING);
        return RoomInviteMapper.toDTOList(invites);
    }
}
