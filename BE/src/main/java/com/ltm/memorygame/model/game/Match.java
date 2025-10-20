package com.ltm.memorygame.model.game;

import com.ltm.memorygame.model.enums.MatchStatus;
import com.ltm.memorygame.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "game_match")
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

    @Column(name = "player1_score")
    private int player1Score;

    @Column(name = "player2_score")
    private int player2Score;

    @ManyToOne
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Column(name = "board_size")
    private String boardSize;

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

    public User getOpponent(User user) {
        if (user.equals(player1)) return player2;
        else return player1;
    }

    public int getScoreFor(User user) {
        if (user == null) return 0;
        if (user.equals(player1)) return player1Score;
        if (user.equals(player2)) return player2Score;
        return 0;
    }

    public Long getWinnerId() {
        if (player1Score > player2Score) return player1.getId();
        if (player2Score > player1Score) return player2.getId();
        return null;
    }

    public MatchStatus getResultFor(User user) {
        if (user == null) return null;
        Long winnerId = getWinnerId();
        if (winnerId == null) return MatchStatus.PLAYING;
        return winnerId.equals(user.getId()) ? MatchStatus.WIN : MatchStatus.LOSE;
    }
}
