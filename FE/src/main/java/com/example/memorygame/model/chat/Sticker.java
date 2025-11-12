package com.example.memorygame.model.chat;

/**
 * Model cho sticker trong chat
 */
public class Sticker {
    private Long id;
    private String stickerPath;
    private String type; // NORMAL hoặc MATCH

    public Sticker() {
    }

    public Sticker(Long id, String stickerPath) {
        this.id = id;
        this.stickerPath = stickerPath;
    }

    public Sticker(Long id, String stickerPath, String type) {
        this.id = id;
        this.stickerPath = stickerPath;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

