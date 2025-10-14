package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.RoomResponseDTO;
import com.ltm.memorygame.model.game.Room;

import java.util.List;
import java.util.stream.Collectors;

public class RoomMapper {

    public static RoomResponseDTO toDTO(Room room) {
        RoomResponseDTO dto = new RoomResponseDTO();
        dto.setId(room.getId());
        dto.setHostId(room.getHost().getId());
        dto.setGuestId(room.getGuest() != null ? room.getGuest().getId() : null);
        dto.setStatus(room.getStatus());
        return dto;
    }

    public static List<RoomResponseDTO> toDTOList(List<Room> rooms) {
        return rooms.stream().map(RoomMapper::toDTO).collect(Collectors.toList());
    }
}
