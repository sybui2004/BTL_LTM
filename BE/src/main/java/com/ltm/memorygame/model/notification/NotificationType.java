package com.ltm.memorygame.model.notification;

import com.ltm.memorygame.model.enums.NotificationTypeName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_type")
@Setter
@Getter
public class NotificationType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private NotificationTypeName name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
