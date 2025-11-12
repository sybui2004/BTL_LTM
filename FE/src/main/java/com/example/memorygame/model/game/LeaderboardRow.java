package com.example.memorygame.model.game;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * Data model for leaderboard table row
 */
public class LeaderboardRow {
    private final StringProperty rank;
    private final StringProperty name;
    private final StringProperty elo;
    private final StringProperty wins;
    private Image avatar;
    private Long userId;

    public LeaderboardRow(String rank, String name, String elo, String wins, Image avatar, Long userId) {
        this.rank = new SimpleStringProperty(rank);
        this.name = new SimpleStringProperty(name);
        this.elo = new SimpleStringProperty(elo);
        this.wins = new SimpleStringProperty(wins);
        this.avatar = avatar;
        this.userId = userId;
    }

    public String getRank() {
        return rank.get();
    }

    public StringProperty rankProperty() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank.set(rank);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getElo() {
        return elo.get();
    }

    public StringProperty eloProperty() {
        return elo;
    }

    public void setElo(String elo) {
        this.elo.set(elo);
    }

    public String getWins() {
        return wins.get();
    }

    public StringProperty winsProperty() {
        return wins;
    }

    public void setWins(String wins) {
        this.wins.set(wins);
    }

    public Image getAvatar() {
        return avatar;
    }

    public void setAvatar(Image avatar) {
        this.avatar = avatar;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}


