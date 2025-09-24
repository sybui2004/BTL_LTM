package com.ltm.memorygame.model.game;

import com.ltm.memorygame.model.enums.MatchStatus;
import com.ltm.memorygame.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "Match")
@Getter
@Setter
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    private int score_winner;

    private int score_lose;

    @ManyToOne
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Column(name = "board_size")
    private String boardSize;

    private Long winner_id;

    @Column(name = "time_per_move")
    private int timePerMove;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime = new Date();

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.PLAYING;
}
