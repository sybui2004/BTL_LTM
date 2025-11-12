package com.ltm.memorygame.dao.notification;

import com.ltm.memorygame.model.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
