package com.ltm.memorygame.config;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * Xử lý lỗi toàn cục cho các message STOMP/WebSocket trong toàn bộ ứng dụng.
 * Mọi lỗi xảy ra trong các @MessageMapping controller sẽ được đẩy về /user/queue/errors.
 */
@Slf4j
@ControllerAdvice
public class WebSocketGlobalExceptionHandler {

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public String handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ex.getMessage();
    }

    @MessageExceptionHandler(NumberFormatException.class)
    @SendToUser("/queue/errors")
    public String handleNumberFormat(NumberFormatException ex) {
        log.warn("Invalid principal/user id: {}", ex.getMessage());
        return "Invalid principal or user id: " + ex.getMessage();
    }

    @MessageExceptionHandler(Throwable.class)
    @SendToUser("/queue/errors")
    public String handleGeneric(Throwable ex) {
        log.error("Unexpected WebSocket error: {}", ex.getMessage(), ex);
        return "Unexpected WebSocket error: " + ex.getMessage();
    }
}
