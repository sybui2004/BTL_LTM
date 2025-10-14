package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.RoomInviteResponseDTO;
import com.ltm.memorygame.model.game.RoomInvite;

import java.util.List;
import java.util.stream.Collectors;

public class RoomInviteMapper {

    public static RoomInviteResponseDTO toDTO(RoomInvite invite) {
        if (invite == null) return null;

        RoomInviteResponseDTO dto = new RoomInviteResponseDTO();
        dto.setId(invite.getId());
        dto.setRoomId(invite.getRoom() != null ? invite.getRoom().getId() : null);
        dto.setSenderId(invite.getSender() != null ? invite.getSender().getId() : null);
        dto.setSenderName(invite.getSender() != null ? invite.getSender().getUsername() : null);
        dto.setReceiverId(invite.getReceiver() != null ? invite.getReceiver().getId() : null);
        dto.setReceiverName(invite.getReceiver() != null ? invite.getReceiver().getUsername() : null);
        dto.setStatus(invite.getStatus());
        dto.setCreatedAt(invite.getCreatedAt());
        return dto;
    }

    public static List<RoomInviteResponseDTO> toDTOList(List<RoomInvite> invites) {
        return invites.stream().map(RoomInviteMapper::toDTO).collect(Collectors.toList());
    }
}
