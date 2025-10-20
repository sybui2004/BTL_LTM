package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findBySenderAndReceiverAndIsDeletedFalse(User sender, User receiver);

    List<Friend> findBySenderOrReceiverAndIsDeletedFalse(User sender, User receiver);

    List<Friend> findBySenderOrReceiverAndStatusAndIsDeletedFalse(User sender, User receiver, FriendStatus status);
}
