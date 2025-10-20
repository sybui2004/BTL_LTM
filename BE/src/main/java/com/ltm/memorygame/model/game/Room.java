package com.ltm.memorygame.model.game;

import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "room")
@Getter
@Setter
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne
    @JoinColumn(name = "guest_id")
    private User guest;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
