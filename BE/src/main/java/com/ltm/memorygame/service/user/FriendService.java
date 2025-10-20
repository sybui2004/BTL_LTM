package com.ltm.memorygame.service.user;

import com.ltm.memorygame.dao.user.FriendRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.friend.response.FriendListDTO;
import com.ltm.memorygame.dto.friend.response.FriendResponseDTO;
import com.ltm.memorygame.mapper.FriendMapper;
import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final FriendMapper friendMapper;

    @Transactional
    public FriendResponseDTO request(Long currentUserId, Long toUserId) {
        if (Objects.equals(currentUserId, toUserId)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("Sender not found"));
        User receiver = userRepository.findById(toUserId)
                .orElseThrow(() -> new NoSuchElementException("Receiver not found"));

        Optional<Friend> existedAB = friendRepository.findBySenderAndReceiverAndIsDeletedFalse(sender, receiver);
        Optional<Friend> existedBA = friendRepository.findBySenderAndReceiverAndIsDeletedFalse(receiver, sender);

        if (existedAB.isPresent() || existedBA.isPresent()) {
            throw new IllegalStateException("Relation already exists (PENDING/ACCEPTED)");
        }

        Friend fr = friendMapper.toFriend(sender, receiver);
        friendRepository.save(fr);

        return friendMapper.toFriendResponseDTO(fr);
    }

    @Transactional
    public FriendResponseDTO accept(Long currentUserId, Long friendRecordId) {
        Friend fr = friendRepository.findById(friendRecordId)
                .orElseThrow(() -> new NoSuchElementException("Friend request not found"));
        if (!Objects.equals(fr.getReceiver().getId(), currentUserId)) {
            throw new IllegalStateException("You are not the recipient of this request");
        }
        if (fr.isDeleted()) {
            throw new IllegalStateException("The request has been cancelled");
        }
        fr.setStatus(FriendStatus.ACCEPTED);
        fr.setAcceptedAt(new Date());
        friendRepository.save(fr);

        return friendMapper.toFriendResponseDTO(fr);
    }

    @Transactional
    public void rejectOrCancel(Long currentUserId, Long friendRecordId) {
        Friend fr = friendRepository.findById(friendRecordId)
                .orElseThrow(() -> new NoSuchElementException("Friend request not found"));

        boolean isOwner = Objects.equals(fr.getSender().getId(), currentUserId)
                || Objects.equals(fr.getReceiver().getId(), currentUserId);
        if (!isOwner) throw new IllegalStateException("You are not allowed to modify this request");

        fr.setDeleted(true);
        friendRepository.save(fr);
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendUserId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        User other = userRepository.findById(friendUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Friend> relations = friendRepository.findBySenderOrReceiverAndIsDeletedFalse(me, me);
        for (Friend fr : relations) {
            boolean match = (Objects.equals(fr.getSender().getId(), me.getId()) && Objects.equals(fr.getReceiver().getId(), other.getId()))
                    || (Objects.equals(fr.getReceiver().getId(), me.getId()) && Objects.equals(fr.getSender().getId(), other.getId()));
            if (match && fr.getStatus() == FriendStatus.ACCEPTED) {
                fr.setDeleted(true);
                friendRepository.save(fr);
                return;
            }
        }
        throw new IllegalStateException("No accepted friendship found to remove");
    }

    @Transactional(readOnly = true)
    public FriendListDTO getAllForUser(Long currentUserId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Friend> all = friendRepository.findBySenderOrReceiverAndIsDeletedFalse(me, me);
        return friendMapper.toFriendListDTO(me, all);
    }
}
