package com.ltm.memorygame.dto.chat.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//snapshot toàn cục cho màn hình danh sách (friends/user list)
public class PresenceSnapshotResponse {
    private List<PresenceUserDto> users;
    private Instant serverTime;
}
