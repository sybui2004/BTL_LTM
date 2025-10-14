package com.ltm.memorygame.dto.user.response;

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
public class UserPresenceDTO {
    private Long userId;
    private String displayName; 
    private String username;    
    private String avatarUrl;   
    private String status;     
}
