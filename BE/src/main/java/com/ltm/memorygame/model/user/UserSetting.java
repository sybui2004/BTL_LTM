package com.ltm.memorygame.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "UserSetting")
@Getter
@Setter
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long user_id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "music_volume")
    private int musicVolume = 50;

    @Column(name = "sound_fx_volume")
    private int soundFxVolume = 50;

    @Column(name = "notification_enabled")
    private boolean notification = true;

    private String language = "en";
}
