package com.ltm.memorygame.model.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "notification")
@Setter
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
