package com.ltm.memorygame.service.user;

import java.util.Date;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.user.response.UserPresenceDTO;
import com.ltm.memorygame.mapper.PresenceMapper;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserRepository userRepository;

    
    // Set status cho user (ONLINE | BUSY | OFFLINE)
    
    @Transactional
public UserPresenceDTO setStatus(Long userId, UserStatus status) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

    user.setStatus(status);
    user.setUpdatedAt(new Date());
    User saved = userRepository.save(user);

    UserPresenceDTO dto = PresenceMapper.toDTO(saved);
    
    return dto; // Trả về DTO hoàn chỉnh
}


}
