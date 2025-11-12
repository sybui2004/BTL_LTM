package com.ltm.memorygame.tcp;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TCPMessage {
    private String type;
    private Map<String, Object> data;
    private String sender;
    private String receiver;
    private long timestamp;
    private String status;

    public TCPMessage(String type, Map<String, Object> data, String sender, String receiver) {
        this.type = type;
        this.data = data;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = System.currentTimeMillis();
        this.status = "OK";
    }
}
