package com.ltm.memorygame.service.notification;

import com.ltm.memorygame.model.notification.Notification;
import com.ltm.memorygame.model.notification.NotificationType;
import com.ltm.memorygame.dao.notification.NotificationRepository;
import com.ltm.memorygame.dao.notification.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository typeRepository;

    public void sendNotification(Long senderId, Long receiverId, String typeName, String content) {
        NotificationType type = typeRepository.findAll().stream()
                .filter(t -> t.getName().name().equals(typeName))
                .findFirst().orElseThrow();

        Notification notification = new Notification();
        notification.setSenderId(senderId.intValue());
        notification.setReceiverId(receiverId.intValue());
        notification.setType(type);
        notification.setContent(content);
        notification.setCreatedAt(new Date());

        notificationRepository.save(notification);
    }
}
