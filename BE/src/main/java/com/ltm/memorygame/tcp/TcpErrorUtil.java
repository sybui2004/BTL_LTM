package com.ltm.memorygame.tcp;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpErrorUtil {

    private static final Logger log = LoggerFactory.getLogger(TcpErrorUtil.class);

    // Chuẩn hóa và xử lý Exception, trả về Map sẵn sàng để đóng gói vào TCPMessage
    public static Map<String, Object> getErrorMap(Exception e, String username, String defaultReason) {

        String reason;
        String errorCode;

        if (e instanceof NoSuchElementException) {
            // Lỗi khi không tìm thấy Entity 
            reason = "User not found or resource missing in DB.";
            errorCode = "RESOURCE_NOT_FOUND";

        } else if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            // Lỗi nghiệp vụ 
            reason = e.getMessage(); 
            errorCode = "BUSINESS_LOGIC_ERROR";
        } else {
            // Các lỗi khác (IO, Runtime, v.v.)
            reason = defaultReason; 
            errorCode = "INTERNAL_ERROR";

            log.error("[TCP ERROR] Unhandled exception for user {}: {}", username, e.getMessage(), e);
        }

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("reason", reason);
        errorMap.put("code", errorCode);
        return errorMap;
    }
}