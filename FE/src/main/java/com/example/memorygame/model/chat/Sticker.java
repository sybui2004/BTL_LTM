package com.example.memorygame.model.chat;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Sticker {
    private final LongProperty id;
    private final StringProperty stickerPath; 

    public Sticker(Long id, String stickerPath) {
        this.id = new SimpleLongProperty(id);
        this.stickerPath = new SimpleStringProperty(stickerPath);
    }

    // Getters and Properties
    public LongProperty idProperty() { return id; }
    public StringProperty stickerPathProperty() { return stickerPath; }

    public Long getId() { return id.get(); }
    public String getStickerPath() { return stickerPath.get(); }
}