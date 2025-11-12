package com.ltm.memorygame.service.user;

import com.ltm.memorygame.dao.user.FriendRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.friend.response.FriendListDTO;
import com.ltm.memorygame.dto.friend.response.FriendResponseDTO;
import com.ltm.memorygame.mapper.FriendMapper;
import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.tcp.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final FriendMapper friendMapper;
    @Lazy
    private final TCPServer tcpServer;

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

        // Check if there's already a PENDING request from receiver to sender (reverse
        // direction)
        // If yes, automatically accept it instead of creating a new request
        if (existedBA.isPresent()) {
            Friend existingRequest = existedBA.get();
            if (existingRequest.getStatus() == FriendStatus.PENDING) {
                // Auto-accept the existing request
                existingRequest.setStatus(FriendStatus.ACCEPTED);
                existingRequest.setAcceptedAt(new Date());
                friendRepository.save(existingRequest);

                // Clean up any other active requests between the same two users
                List<Friend> duplicates = friendRepository.findAllBetweenUsersActive(existingRequest.getSender(),
                        existingRequest.getReceiver());
                for (Friend dup : duplicates) {
                    if (!Objects.equals(dup.getId(), existingRequest.getId())) {
                        dup.setDeleted(true);
                        friendRepository.save(dup);
                    }
                }

                // Send real-time TCP notification to both users to refresh their friend lists
                try {
                    tcpServer.sendFriendStatusChangedNotification(
                            existingRequest.getSender().getUsername(),
                            existingRequest.getReceiver().getUsername());
                } catch (Exception e) {
                    System.err.println("[FriendService] Failed to send TCP notification: " + e.getMessage());
                }

                return friendMapper.toFriendResponseDTO(existingRequest);
            } else if (existingRequest.getStatus() == FriendStatus.ACCEPTED) {
                // Already friends
                throw new IllegalStateException("Relation already exists (ACCEPTED)");
            }
        }

        // Check if there's already a request from sender to receiver (same direction)
        if (existedAB.isPresent()) {
            Friend existingRequest = existedAB.get();
            if (existingRequest.getStatus() == FriendStatus.ACCEPTED) {
                throw new IllegalStateException("Relation already exists (ACCEPTED)");
            } else if (existingRequest.getStatus() == FriendStatus.PENDING) {
                throw new IllegalStateException("You have already sent a friend request to this user");
            }
        }

        Friend fr = friendMapper.toFriend(sender, receiver);
        friendRepository.save(fr);

        // Send real-time TCP notification to receiver if online
        try {
            tcpServer.sendFriendRequestNotification(
                    receiver.getUsername(),
                    fr.getId(),
                    sender.getId(),
                    sender.getDisplayName() != null && !sender.getDisplayName().isBlank()
                            ? sender.getDisplayName()
                            : sender.getUsername(),
                    sender.getAvatarUrl() != null ? sender.getAvatarUrl() : "");
        } catch (Exception e) {
            // Log but don't fail the request if TCP notification fails
            System.err.println("[FriendService] Failed to send TCP notification: " + e.getMessage());
        }

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
            // The provided record was cancelled. Try to locate the active pending request
            // between the same two users where current user is the receiver.
            List<Friend> betweenActive = friendRepository.findAllBetweenUsersActive(fr.getSender(), fr.getReceiver());
            Optional<Friend> replacement = betweenActive.stream()
                    .filter(f -> f.getStatus() == FriendStatus.PENDING
                            && Objects.equals(f.getReceiver().getId(), currentUserId))
                    .findFirst();
            if (replacement.isPresent()) {
                fr = replacement.get();
            } else {
                throw new IllegalStateException("The request has been cancelled");
            }
        }
        fr.setStatus(FriendStatus.ACCEPTED);
        fr.setAcceptedAt(new Date());
        friendRepository.save(fr);

        // Clean up any other active requests between the same two users
        List<Friend> duplicates = friendRepository.findAllBetweenUsersActive(fr.getSender(), fr.getReceiver());
        for (Friend dup : duplicates) {
            if (!Objects.equals(dup.getId(), fr.getId())) {
                dup.setDeleted(true);
                friendRepository.save(dup);
            }
        }

        // Send real-time TCP notification to both users to refresh their friend lists
        try {
            tcpServer.sendFriendStatusChangedNotification(
                    fr.getSender().getUsername(),
                    fr.getReceiver().getUsername());
        } catch (Exception e) {
            System.err.println("[FriendService] Failed to send TCP notification: " + e.getMessage());
        }

        return friendMapper.toFriendResponseDTO(fr);
    }

    @Transactional
    public void rejectOrCancel(Long currentUserId, Long friendRecordId) {
        Friend fr = friendRepository.findById(friendRecordId)
                .orElseThrow(() -> new NoSuchElementException("Friend request not found"));

        boolean isOwner = Objects.equals(fr.getSender().getId(), currentUserId)
                || Objects.equals(fr.getReceiver().getId(), currentUserId);
        if (!isOwner)
            throw new IllegalStateException("You are not allowed to modify this request");

        // Soft delete this request and any other active requests between the same two
        // users
        List<Friend> between = friendRepository.findAllBetweenUsersActive(fr.getSender(), fr.getReceiver());
        boolean any = false;
        for (Friend f : between) {
            f.setDeleted(true);
            friendRepository.save(f);
            any = true;
        }
        if (!any) {
            // Fallback to delete the current one if query failed to find it
            fr.setDeleted(true);
            friendRepository.save(fr);
        }

        // Send real-time TCP notification to both users to refresh their friend lists
        try {
            tcpServer.sendFriendStatusChangedNotification(
                    fr.getSender().getUsername(),
                    fr.getReceiver().getUsername());
        } catch (Exception e) {
            System.err.println("[FriendService] Failed to send TCP notification: " + e.getMessage());
        }
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendUserId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        User other = userRepository.findById(friendUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Friend> relations = friendRepository.findActiveByUser(me);
        boolean removedAny = false;
        for (Friend fr : relations) {
            boolean match = (Objects.equals(fr.getSender().getId(), me.getId())
                    && Objects.equals(fr.getReceiver().getId(), other.getId()))
                    || (Objects.equals(fr.getReceiver().getId(), me.getId())
                            && Objects.equals(fr.getSender().getId(), other.getId()));
            if (match) {
                fr.setDeleted(true);
                friendRepository.save(fr);
                removedAny = true;

                // Send real-time TCP notification to both users to refresh their friend lists
                try {
                    tcpServer.sendFriendStatusChangedNotification(
                            fr.getSender().getUsername(),
                            fr.getReceiver().getUsername());
                } catch (Exception e) {
                    System.err.println("[FriendService] Failed to send TCP notification: " + e.getMessage());
                }
            }
        }
        if (!removedAny) {
            throw new IllegalStateException("No relationship found to remove");
        }
    }

    @Transactional(readOnly = true)
    public FriendListDTO getAllForUser(Long currentUserId) {
        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Friend> all = friendRepository.findActiveByUser(me);
        return friendMapper.toFriendListDTO(me, all);
    }
}
