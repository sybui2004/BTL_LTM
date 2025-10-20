package com.ltm.memorygame.service.notification;

import com.ltm.memorygame.model.notification.Notification;
import com.ltm.memorygame.model.notification.NotificationType;
import com.ltm.memorygame.dao.notification.NotificationRepository;
import com.ltm.memorygame.dao.notification.NotificationTypeRepository;
import com.ltm.memorygame.model.enums.NotificationTypeName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository typeRepository;

    public void sendNotification(Long senderId, Long receiverId, String typeName, String content) {
        NotificationTypeName enumName = NotificationTypeName.valueOf(typeName);
        NotificationType type = typeRepository.findByName(enumName)
                .orElseThrow();

        Notification notification = new Notification();
        notification.setSenderId(senderId);
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setContent(content);
        notification.setCreatedAt(new Date());

        notificationRepository.save(notification);
    }
}
