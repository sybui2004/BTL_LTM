package com.ltm.memorygame.tcp;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TCPMessage {
    private String type; // JOIN, MOVE, EXIT, UPDATE
    private Long roomId;
    private Long playerId;
    private List<Integer> cards; // chỉ dùng cho MOVE
    private String content; // Optional, thông báo, lỗi, kết quả
}
