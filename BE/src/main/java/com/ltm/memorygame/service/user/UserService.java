package com.ltm.memorygame.service.user;

import com.ltm.memorygame.dao.game.MatchRepository;
import com.ltm.memorygame.dao.user.UserRankingProjection;
import com.ltm.memorygame.dto.auth.request.RegisterRequest;
import com.ltm.memorygame.dto.friend.response.FriendDTO;
import com.ltm.memorygame.dto.user.response.UserProfileDTO;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.mapper.UserMapper;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.model.user.UserSetting;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.event.UserStatusChangedEvent;
import com.ltm.memorygame.event.UserProfileUpdatedEvent;
import com.ltm.memorygame.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponseDTO createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordHasher.hashPassword(request.getPassword()));
        user.setEmail(request.getEmail());

        user.setCreatedAt(new Date());
        user.setStatus(UserStatus.OFFLINE);

        UserSetting setting = new UserSetting();
        setting.setUser(user);
        user.setUserSetting(setting);

        User saved = userRepository.save(user);

        String hashedId = String.format("%08d", Math.abs(saved.getId().hashCode()) % 100_000_000);
        saved.setDisplayName("user" + hashedId);
        saved.setAvatarUrl("/images/default-avatar.png");

        User updated = userRepository.save(saved);

        return userMapper.toUserResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUser(Long userId) {
        return userMapper.toUserResponseDTO(getEntityById(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUser() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId) {
        User user = getEntityById(userId);

        List<Match> matches = matchRepository.findTop20ByPlayer1OrPlayer2OrderByStartTimeDesc(user, user);
        System.out.println("[UserService] Found " + matches.size() + " total matches for user " + userId);

        // Filter out matches that are still playing (only include finished matches)
        List<Match> finishedMatches = matches.stream()
                .filter(match -> match.getStatus() != com.ltm.memorygame.model.enums.MatchStatus.PLAYING)
                .collect(java.util.stream.Collectors.toList());

        System.out.println(
                "[UserService] After filtering, " + finishedMatches.size() + " finished matches for user " + userId);

        return userMapper.toUserProfileDTO(user, finishedMatches);
    }

    @Transactional(readOnly = true)
    public User getEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserRankingProjection> getRanking() {
        return userRepository.getUserRankingNative();
    }

    @Transactional
    public void setStatus(Long userId, UserStatus status) {
        User user = getEntityById(userId);
        user.setStatus(status);
        userRepository.save(user);

        boolean online = (status == UserStatus.ONLINE || status == UserStatus.BUSY);
        eventPublisher.publishEvent(new UserStatusChangedEvent(this, user.getUsername(), online));
    }

    @Transactional
    public UserResponseDTO updateProfile(Long userId, String displayName, String avatarUrl) {
        User user = getEntityById(userId);

        // Update display name if provided
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }

        // Update avatar URL if provided
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }

        // Update updatedAt timestamp
        user.setUpdatedAt(new Date());

        User updatedUser = userRepository.save(user);

        // Publish event to notify friends that profile has been updated
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(this, updatedUser.getUsername(), updatedUser.getId()));

        return userMapper.toUserResponseDTO(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = getEntityById(userId);
        user.setPassword(passwordHasher.hashPassword(newPassword));
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<FriendDTO> searchUsers(String q) {
        return userRepository.searchByPrefix(q).stream()
                .map(userMapper::toFriendDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendDTO> searchUsersExcluding(String q, Long excludeUserId) {
        List<User> users = excludeUserId == null
                ? userRepository.searchByPrefix(q)
                : userRepository.searchByPrefixExcluding(q, excludeUserId);
        return users.stream().map(userMapper::toFriendDTO).toList();
    }
}
