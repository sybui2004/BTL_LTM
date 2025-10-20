package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.friend.response.FriendDTO;
import com.ltm.memorygame.dto.friend.response.FriendListDTO;
import com.ltm.memorygame.dto.friend.response.FriendResponseDTO;
import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FriendMapper {

    private final UserMapper userMapper;

    public FriendResponseDTO toFriendResponseDTO(Friend fr) {
        if (fr == null) return null;

        return FriendResponseDTO.builder()
                .id(fr.getId())
                .senderId(fr.getSender().getId())
                .receiverId(fr.getReceiver().getId())
                .status(fr.getStatus())
                .build();
    }

    public Friend toFriend(User sender, User receiver) {
        Friend fr = new Friend();
        fr.setSender(sender);
        fr.setReceiver(receiver);
        fr.setStatus(FriendStatus.PENDING);
        return fr;
    }

    public FriendListDTO toFriendListDTO(User me, List<Friend> allRelations) {
        List<FriendDTO> friends = new ArrayList<>();
        List<FriendDTO> incoming = new ArrayList<>();
        List<FriendDTO> outgoing = new ArrayList<>();

        for (Friend fr : allRelations) {
            User other = Objects.equals(fr.getSender().getId(), me.getId())
                    ? fr.getReceiver()
                    : fr.getSender();

            if (fr.getStatus() == FriendStatus.ACCEPTED) {
                friends.add(userMapper.toFriendDTO(other));
            } else if (fr.getStatus() == FriendStatus.PENDING) {
                if (Objects.equals(fr.getReceiver().getId(), me.getId())) {
                    incoming.add(userMapper.toFriendDTO(other));
                } else {
                    outgoing.add(userMapper.toFriendDTO(other));
                }
            }
        }

        return FriendListDTO.builder()
                .friends(friends)
                .incomingRequest(incoming)
                .outgoingRequest(outgoing)
                .build();
    }
}
