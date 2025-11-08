package com.example.memorygame.controller.chat;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.example.memorygame.model.chat.Sticker;
import com.example.memorygame.utils.ChatApi;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Sticker picker popup component
 * Displays stickers in a 3x3 grid with scrolling support
 */
public class StickerPicker extends StackPane {
    private static final int COLUMNS = 3;
    private static final int VISIBLE_ROWS = 3;
    private static final double STICKER_SIZE = 80.0;
    private static final double PADDING = 10.0;
    private static final double GAP = 8.0;
    
    private final GridPane gridPane;
    private final ScrollPane scrollPane;
    private Consumer<Sticker> onStickerSelected;
    
    public StickerPicker() {
        this.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #ccc; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );
        
        // Create grid for stickers
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(PADDING));
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setAlignment(Pos.CENTER);
        
        // Create scroll pane
        scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent;"
        );
        
        // Calculate preferred size: 3 columns * sticker size + gaps + padding
        double prefWidth = (COLUMNS * STICKER_SIZE) + ((COLUMNS - 1) * GAP) + (2 * PADDING);
        double prefHeight = (VISIBLE_ROWS * STICKER_SIZE) + ((VISIBLE_ROWS - 1) * GAP) + (2 * PADDING);
        
        scrollPane.setPrefWidth(prefWidth);
        scrollPane.setPrefHeight(prefHeight);
        scrollPane.setMinWidth(prefWidth);
        scrollPane.setMinHeight(prefHeight);
        scrollPane.setMaxWidth(prefWidth);
        scrollPane.setMaxHeight(prefHeight);
        
        this.getChildren().add(scrollPane);
        
        // Load stickers asynchronously
        loadStickers();
    }
    
    private void loadStickers() {
        // Load stickers in background thread
        new Thread(() -> {
            List<Sticker> stickers = ChatApi.fetchStickers();
            Platform.runLater(() -> {
                displayStickers(stickers);
            });
        }).start();
    }
    
    private void displayStickers(List<Sticker> stickers) {
        gridPane.getChildren().clear();
        
        if (stickers == null || stickers.isEmpty()) {
            return;
        }
        
        int row = 0;
        int col = 0;
        
        for (Sticker sticker : stickers) {
            if (sticker == null) continue;
            
            StackPane stickerContainer = createStickerButton(sticker);
            gridPane.add(stickerContainer, col, row);
            
            col++;
            if (col >= COLUMNS) {
                col = 0;
                row++;
            }
        }
    }
    
    private StackPane createStickerButton(Sticker sticker) {
        StackPane container = new StackPane();
        container.setPrefSize(STICKER_SIZE, STICKER_SIZE);
        container.setMinSize(STICKER_SIZE, STICKER_SIZE);
        container.setMaxSize(STICKER_SIZE, STICKER_SIZE);
        container.setStyle(
            "-fx-background-color: #f5f5f5; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 6; " +
            "-fx-border-width: 1;"
        );
        
        // Clip to rounded rectangle
        Rectangle clip = new Rectangle(STICKER_SIZE, STICKER_SIZE);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        container.setClip(clip);
        
        // Load image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(STICKER_SIZE - 8);
        imageView.setFitHeight(STICKER_SIZE - 8);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        
        try {
            String imageUrl = sticker.getStickerPath();
            if (imageUrl != null && !imageUrl.isBlank()) {
                // If it's a relative path (starts with /static/), prepend backend URL
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    if (imageUrl.startsWith("/static/")) {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    } else if (!imageUrl.startsWith("/")) {
                        imageUrl = "http://localhost:8080/static/stickers/" + imageUrl;
                    } else {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    }
                }
                Image image = new Image(imageUrl, true);
                imageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("[StickerPicker] Failed to load sticker image: " + e.getMessage());
        }
        
        container.getChildren().add(imageView);
        
        // Hover effect
        container.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            container.setStyle(
                "-fx-background-color: #e0e0e0; " +
                "-fx-background-radius: 6; " +
                "-fx-border-color: #bbb; " +
                "-fx-border-radius: 6; " +
                "-fx-border-width: 1;"
            );
        });
        
        container.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            container.setStyle(
                "-fx-background-color: #f5f5f5; " +
                "-fx-background-radius: 6; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-radius: 6; " +
                "-fx-border-width: 1;"
            );
        });
        
        // Click handler
        container.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onStickerSelected != null) {
                onStickerSelected.accept(sticker);
            }
        });
        
        return container;
    }
    
    public void setOnStickerSelected(Consumer<Sticker> handler) {
        this.onStickerSelected = handler;
    }
}

