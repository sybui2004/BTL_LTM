package com.ltm.memorygame.controller;

import com.ltm.memorygame.dto.friend.request.CreateFriendRequest;
import com.ltm.memorygame.dto.friend.response.FriendListDTO;
import com.ltm.memorygame.dto.friend.response.FriendResponseDTO;
import com.ltm.memorygame.service.user.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import com.ltm.memorygame.security.AuthUtils;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/{currentUserId}")
    public ResponseEntity<FriendListDTO> list(@PathVariable Long currentUserId) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !Objects.equals(authId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(friendService.getAllForUser(currentUserId));
    }

    @PostMapping("/{currentUserId}/requests")
    public ResponseEntity<FriendResponseDTO> create(@PathVariable Long currentUserId,
                                                    @Valid @RequestBody CreateFriendRequest req) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !Objects.equals(authId, currentUserId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(friendService.request(currentUserId, req.getToUserId()), HttpStatus.CREATED);
    }

    @PostMapping("/{currentUserId}/requests/{id}/accept")
    public ResponseEntity<FriendResponseDTO> accept(@PathVariable Long currentUserId,
                                                    @PathVariable Long id) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !Objects.equals(authId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(friendService.accept(currentUserId, id));
    }

    @PostMapping("/{currentUserId}/requests/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long currentUserId,
                                       @PathVariable Long id) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !Objects.equals(authId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        friendService.rejectOrCancel(currentUserId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{currentUserId}/{friendUserId}")
    public ResponseEntity<Void> remove(@PathVariable Long currentUserId,
                                       @PathVariable Long friendUserId) {
        Long authId = AuthUtils.getAuthenticatedUserId();
        if (authId == null || !Objects.equals(authId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        friendService.removeFriend(currentUserId, friendUserId);
        return ResponseEntity.noContent().build();
    }

    
}
