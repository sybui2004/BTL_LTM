package com.ltm.memorygame.service.room;

import com.ltm.memorygame.dao.game.RoomInviteRepository;
import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.mapper.RoomInviteMapper;
import com.ltm.memorygame.model.enums.InviteStatus;
import com.ltm.memorygame.model.game.RoomInvite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final RoomInviteRepository inviteRepository;

    @Transactional(readOnly = true)
    public boolean existsPendingInvite(Long roomId, Long receiverId) {
        return inviteRepository.existsByRoomIdAndReceiverIdAndStatus(roomId, receiverId, InviteStatus.PENDING);
    }

    @Transactional
    public void save(RoomInvite invite) {
        inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public Optional<RoomInvite> findPendingByRoomAndReceiver(Long roomId, Long receiverId) {
        return inviteRepository.findByRoomIdAndReceiverIdAndStatus(roomId, receiverId, InviteStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<RoomInviteResponseDTO> getPendingInvites(Long receiverId) {
        List<RoomInvite> invites = inviteRepository.findByReceiverIdAndStatus(receiverId, InviteStatus.PENDING);
        return RoomInviteMapper.toDTOList(invites);
    }
}
