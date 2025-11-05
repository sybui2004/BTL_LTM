package com.example.memorygame.model.game;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Data model for match history table row
 */
public class MatchHistoryRow {
    private final StringProperty opponent;
    private final StringProperty result;
    private final StringProperty eloChange;
    private final StringProperty date;

    public MatchHistoryRow(String opponent, String result, String eloChange, String date) {
        this.opponent = new SimpleStringProperty(opponent);
        this.result = new SimpleStringProperty(result);
        this.eloChange = new SimpleStringProperty(eloChange);
        this.date = new SimpleStringProperty(date);
    }

    public String getOpponent() {
        return opponent.get();
    }

    public StringProperty opponentProperty() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent.set(opponent);
    }

    public String getResult() {
        return result.get();
    }

    public StringProperty resultProperty() {
        return result;
    }

    public void setResult(String result) {
        this.result.set(result);
    }

    public String getEloChange() {
        return eloChange.get();
    }

    public StringProperty eloChangeProperty() {
        return eloChange;
    }

    public void setEloChange(String eloChange) {
        this.eloChange.set(eloChange);
    }

    public String getDate() {
        return date.get();
    }

    public StringProperty dateProperty() {
        return date;
    }

    public void setDate(String date) {
        this.date.set(date);
    }
}

