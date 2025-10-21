package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findBySenderAndReceiverAndIsDeletedFalse(User sender, User receiver);

    // NOTE: Be careful with derived query precedence: AND binds tighter than OR.
    // Explicit JPQL ensures (sender = :user OR receiver = :user) is grouped together
    // under isDeleted = false, preventing deleted rows from leaking into results.
    @org.springframework.data.jpa.repository.Query("""
        SELECT f FROM Friend f
        WHERE f.isDeleted = false AND (f.sender = :user OR f.receiver = :user)
    """)
    java.util.List<Friend> findActiveByUser(
            @org.springframework.data.repository.query.Param("user") User user
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT f FROM Friend f
        WHERE f.isDeleted = false AND f.status = :status
          AND (f.sender = :user OR f.receiver = :user)
    """)
    java.util.List<Friend> findActiveByUserAndStatus(
            @org.springframework.data.repository.query.Param("user") User user,
            @org.springframework.data.repository.query.Param("status") FriendStatus status
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT f FROM Friend f
        WHERE f.isDeleted = false AND (
            (f.sender = :a AND f.receiver = :b) OR (f.sender = :b AND f.receiver = :a)
        )
    """)
    java.util.List<Friend> findAllBetweenUsersActive(
            @org.springframework.data.repository.query.Param("a") User a,
            @org.springframework.data.repository.query.Param("b") User b
    );
}
