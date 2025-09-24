package com.ltm.memorygame.model.notification;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "Notification")
@Setter
@Getter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private int senderId;

    @Column(name = "receiver_id", nullable = false)
    private int receiverId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private int typeId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
