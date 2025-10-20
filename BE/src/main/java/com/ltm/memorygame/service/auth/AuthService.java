package com.ltm.memorygame.service.auth;

import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.auth.response.AuthResponse;
import com.ltm.memorygame.dto.auth.request.AuthRequest;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.mapper.UserMapper;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUsername()));

        if (user.getPassword() == null || !passwordHasher.verifyPassword(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Wrong username or password");
        }

        if (!user.getPassword().startsWith("PBKDF2$")) {
            user.setPassword(passwordHasher.hashPassword(req.getPassword()));
        }

        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);

        UserResponseDTO dto = userMapper.toUserResponseDTO(user);
        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, dto);
    }

    /** DEV logout: set OFFLINE theo userId */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        user.setStatus(UserStatus.OFFLINE);
        userRepository.save(user);
    }
}
