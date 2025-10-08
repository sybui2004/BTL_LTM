package com.ltm.memorygame.dto.chat.response;

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

//Snapshot trạng thái một user để hiển thị nhanh bên FE.
public class PresenceUserDto {
    private Long userId;
    private String displayName; 
    private String username;    
    private String avatarUrl;   
    private String status;      // ONLINE | BUSY | OFFLINE
}
