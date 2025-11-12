package com.example.memorygame.model.game;

public class RoomResponseDTO {
    public Long id;
    public Long hostId;
    public Long guestId;
    public String status; // WAITING, READY, PLAYING, FINISHED, DELETED
}

