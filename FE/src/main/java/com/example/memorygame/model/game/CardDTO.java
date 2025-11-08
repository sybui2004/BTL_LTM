package com.example.memorygame.model.game;

/**
 * Card data transfer object
 */
public class CardDTO {
    private Long id;
    private String imagePath;
    
    // Constructors
    public CardDTO() {}
    
    public CardDTO(Long id, String imagePath) {
        this.id = id;
        this.imagePath = imagePath;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    @Override
    public String toString() {
        return "CardDTO{" +
                "id=" + id +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}
