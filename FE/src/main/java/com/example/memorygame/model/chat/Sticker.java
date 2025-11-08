package com.example.memorygame.model.chat;

/**
 * Model cho sticker trong chat
 */
public class Sticker {
    private Long id;
    private String stickerPath;

    public Sticker() {
    }

    public Sticker(Long id, String stickerPath) {
        this.id = id;
        this.stickerPath = stickerPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStickerPath() {
        return stickerPath;
    }

    public void setStickerPath(String stickerPath) {
        this.stickerPath = stickerPath;
    }
}

