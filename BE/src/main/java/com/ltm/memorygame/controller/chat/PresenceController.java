package com.ltm.memorygame.controller.chat;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.chat.response.PresenceSnapshotResponse;
import com.ltm.memorygame.dto.chat.response.PresenceUserDto;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.chat.PresenceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    // REST: snapshot toàn bộ trạng thái hiện tại -------------
    @GetMapping
    public ResponseEntity<PresenceSnapshotResponse> snapshot() {
        Map<Long, String> map = presenceService.getAllStatuses();

        Iterable<User> users = userRepository.findAllById(map.keySet());
        List<PresenceUserDto> items = new ArrayList<>();

        for (User u : users) {
            items.add(PresenceUserDto.builder()
                    .userId(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getDisplayName())
                    .avatarUrl(u.getAvatarUrl())
                    .status(map.getOrDefault(u.getId(), "OFFLINE"))
                    .build());
        }

        PresenceSnapshotResponse body = PresenceSnapshotResponse.builder()
                .serverTime(Instant.now())
                .users(items)
                .build();

        return ResponseEntity.ok(body);
    }

    // STOMP: đặt trạng thái chủ động 
    public static record PresenceSetRequest(@NotNull UserStatus status) {

    }

    @MessageMapping("/presence.set")
    public void setStatus(@Valid @Payload PresenceSetRequest req,
            @Header(name = "X-User-Id", required = false) Long userIdHeader,
            Principal principal) {
        long userId = resolveUserId(userIdHeader, principal);
        if (req.status == null) {
            throw new IllegalArgumentException("status is required: ONLINE/OFFLINE/BUSY");
        }
        switch (req.status) {
            case ONLINE ->
                presenceService.setOnline(userId);
            case OFFLINE ->
                presenceService.setOffline(userId);
            case BUSY ->
                presenceService.setBusy(userId);
        }
    }

    // Set "Busy" khi vao tran
    public static record PresenceRoomBusyRequest(boolean busy) {

    }

    @MessageMapping("/presence.room")
    public void setRoomBusy(@Valid @Payload PresenceRoomBusyRequest req,
            @Header(name = "X-User-Id", required = false) Long userIdHeader,
            Principal principal) {
        long userId = resolveUserId(userIdHeader, principal);
        presenceService.setBusyForRoom(userId, req.busy);
    }

    // ------------- helpers -------------
    private long resolveUserId(Long userIdHeader, Principal principal) {
        if (userIdHeader != null) {
            return userIdHeader;
        }
        if (principal != null && principal.getName() != null) {
            return Long.parseLong(principal.getName());
        }
        throw new IllegalArgumentException("Không xác định được userId (thiếu X-User-Id hoặc Principal).");
    }

}
