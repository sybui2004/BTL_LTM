package com.ltm.memorygame.dto.friend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendListDTO {
    private List<FriendDTO> friends;
    private List<FriendDTO> incomingRequest;
    private List<FriendDTO> outgoingRequest;
}