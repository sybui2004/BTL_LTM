package com.ltm.memorygame.dto.chat.request;

import com.ltm.memorygame.model.enums.MessageType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PrivateMessageRequest {
    @NotNull
    private Long toUserId;

    private Long matchId;

    @Size(max = 2000)
    private String content;
    
    @NotNull
    private MessageType messageType;
    
    private Long stickerId; 

}