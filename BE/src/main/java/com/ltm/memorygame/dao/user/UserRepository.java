package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {
    List<User> findByUsernameContainingIgnoreCase(String keyword);
    List<User> filterByStatus(UserStatus status);
    boolean existsByEmail(String email);
}
