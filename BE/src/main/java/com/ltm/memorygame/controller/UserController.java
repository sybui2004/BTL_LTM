package com.ltm.memorygame.controller;

import com.ltm.memorygame.dao.user.UserRankingProjection;
import com.ltm.memorygame.dto.friend.response.FriendDTO;
import com.ltm.memorygame.dto.user.request.SetStatusRequest;
import com.ltm.memorygame.dto.user.request.UpdateProfileRequest;
import com.ltm.memorygame.dto.user.request.ChangePasswordRequest;
import jakarta.validation.Valid;
import com.ltm.memorygame.dto.user.response.UserProfileDTO;
import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import com.ltm.memorygame.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ltm.memorygame.security.AuthUtils;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUser() {
        return ResponseEntity.ok(userService.getAllUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<UserRankingProjection>> getRanking() {
        return ResponseEntity.ok(userService.getRanking());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> setStatus(@PathVariable Long id,
                                          @Valid @RequestBody SetStatusRequest body) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.setStatus(id, body.getStatus());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateProfileRequest body) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponseDTO updatedUser = userService.updateProfile(id, body.getDisplayName(), body.getAvatarUrl());
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @Valid @RequestBody ChangePasswordRequest body) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !authId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.changePassword(id, body.getPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<FriendDTO>> search(@RequestParam String q) {
        Long excludeId = AuthUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(userService.searchUsersExcluding(q, excludeId));
    }
}
