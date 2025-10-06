package com.ltm.memorygame.dao.game;

import com.ltm.memorygame.model.enums.InviteStatus;
import com.ltm.memorygame.model.game.RoomInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomInviteRepository extends JpaRepository<RoomInvite, Long> {

    List<RoomInvite> findByReceiverIdAndStatus(Long receiverId, InviteStatus status);

    Optional<RoomInvite> findByRoomIdAndReceiverIdAndStatus(Long roomId, Long receiverId, InviteStatus status);

    boolean existsByRoomIdAndReceiverIdAndStatus(Long roomId, Long receiverId, InviteStatus status);
}
