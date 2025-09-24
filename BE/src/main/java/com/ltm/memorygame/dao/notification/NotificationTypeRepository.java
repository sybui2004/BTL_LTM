package com.ltm.memorygame.dao.notification;

import com.ltm.memorygame.model.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
}
