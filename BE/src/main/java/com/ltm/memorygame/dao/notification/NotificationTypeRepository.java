package com.ltm.memorygame.dao.notification;

import com.ltm.memorygame.model.notification.NotificationType;
import com.ltm.memorygame.model.enums.NotificationTypeName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
    Optional<NotificationType> findByName(NotificationTypeName name);
}
