package com.ltm.memorygame.dto.friend.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendListDTO {
    private List<FriendDTO> friends;
    private List<FriendDTO> incomingRequest;
    private List<FriendDTO> outgoingRequest;
}