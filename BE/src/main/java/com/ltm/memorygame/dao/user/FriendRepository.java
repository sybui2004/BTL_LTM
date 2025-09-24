package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.user.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRepository extends JpaRepository<Friend, Long> {

}
