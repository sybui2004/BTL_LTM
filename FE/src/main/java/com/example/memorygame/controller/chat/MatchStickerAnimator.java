package com.example.memorygame.controller.chat;

import java.util.function.Consumer;

import com.example.memorygame.utils.SoundManager;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;

/**
 * Handles the animation of a sticker moving from one player's header to another.
 * Creates an S-curve path and combines path, scale, and fade transitions.
 */
public class MatchStickerAnimator {

    private static final double ANIMATION_DURATION_SECONDS = 3.0;
    private static final double STICKER_START_SCALE = 0.6;
    private static final double STICKER_PEAK_SCALE = 2.0;
    private static final double STICKER_END_SCALE = 1.2;

    /**
     * Animates a sticker from a start node to an end node within a pane.
     *
     * @param stickerView    The ImageView of the sticker to animate.
     * @param animationLayer The Pane where the animation will be rendered.
     * @param startNode      The Node from which the animation starts.
     * @param endNode        The Node where the animation ends.
     * @param isFromMe       Determines the curve direction (true for up-right, false for down-left).
     * @param onFinish       A callback to execute when the animation is complete.
     */
    public static void animate(
            ImageView stickerView,
            Pane animationLayer,
            Node startNode,
            Node endNode,
            boolean isFromMe,
            Consumer<Void> onFinish) {
        animateWithRetry(stickerView, animationLayer, startNode, endNode, isFromMe, onFinish, 0);
    }

    // Internal: retry logic
    private static void animateWithRetry(
            ImageView stickerView,
            Pane animationLayer,
            Node startNode,
            Node endNode,
            boolean isFromMe,
            Consumer<Void> onFinish,
            int retryCount) {
        // 1. Add sticker to the animation layer and hide it initially
        if (!animationLayer.getChildren().contains(stickerView)) {
            animationLayer.getChildren().add(stickerView);
        }
        stickerView.setOpacity(0.0);
        stickerView.setMouseTransparent(true);

        // 2. Calculate start and end points relative to the animationLayer
        Scene scene = animationLayer.getScene();
        if (scene == null) {
            // Nếu scene chưa sẵn sàng, thử lại tối đa 10 lần, mỗi lần cách nhau 20ms
            if (retryCount < 10) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {}
                    animateWithRetry(stickerView, animationLayer, startNode, endNode, isFromMe, onFinish, retryCount + 1);
                });
            } else {
                animationLayer.getChildren().remove(stickerView);
            }
            return;
        }
        double sceneWidth = scene.getWidth();
        double sceneHeight = scene.getHeight();

        Point2D startInLayer, endInLayer;
        if (isFromMe) {
            double startX = sceneWidth * 0.15;
            double startY = sceneHeight * 0.80;
            double endX = sceneWidth * 0.15;
            double endY = sceneHeight * 0.10;
            startInLayer = animationLayer.sceneToLocal(startX, startY);
            endInLayer = animationLayer.sceneToLocal(endX, endY);
        } else {
            double startX = sceneWidth * 0.85;
            double startY = sceneHeight * 0.20;
            double endX = sceneWidth * 0.85;
            double endY = sceneHeight * 0.90;
            startInLayer = animationLayer.sceneToLocal(startX, startY);
            endInLayer = animationLayer.sceneToLocal(endX, endY);
        }

        stickerView.setTranslateX(startInLayer.getX() - stickerView.getFitWidth() / 2);
        stickerView.setTranslateY(startInLayer.getY() - stickerView.getFitHeight() / 2);

        Path path = createWavePath(startInLayer, endInLayer, isFromMe);

        PathTransition pathTransition = new PathTransition();
        pathTransition.setDuration(Duration.seconds(ANIMATION_DURATION_SECONDS));
        pathTransition.setNode(stickerView);
        pathTransition.setPath(path);
        pathTransition.setOrientation(PathTransition.OrientationType.NONE);
        pathTransition.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(ANIMATION_DURATION_SECONDS), stickerView);
        scaleTransition.setFromX(STICKER_START_SCALE);
        scaleTransition.setFromY(STICKER_START_SCALE);
        scaleTransition.setToX(STICKER_END_SCALE);
        scaleTransition.setToY(STICKER_END_SCALE);

        KeyValue peakScaleX = new KeyValue(stickerView.scaleXProperty(), STICKER_PEAK_SCALE);
        KeyValue peakScaleY = new KeyValue(stickerView.scaleYProperty(), STICKER_PEAK_SCALE);
        KeyFrame peakFrame = new KeyFrame(Duration.seconds(ANIMATION_DURATION_SECONDS / 2), peakScaleX, peakScaleY);

        Timeline scaleTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(stickerView.scaleXProperty(), STICKER_START_SCALE), new KeyValue(stickerView.scaleYProperty(), STICKER_START_SCALE)),
            peakFrame,
            new KeyFrame(Duration.seconds(ANIMATION_DURATION_SECONDS), new KeyValue(stickerView.scaleXProperty(), STICKER_END_SCALE), new KeyValue(stickerView.scaleYProperty(), STICKER_END_SCALE))
        );

        Timeline fadeTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(stickerView.opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.15), new KeyValue(stickerView.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(ANIMATION_DURATION_SECONDS - 0.4), new KeyValue(stickerView.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(ANIMATION_DURATION_SECONDS), new KeyValue(stickerView.opacityProperty(), 0.0))
        );

        ParallelTransition parallelTransition = new ParallelTransition(stickerView, pathTransition, scaleTimeline, fadeTimeline);

        parallelTransition.setOnFinished(event -> {
            animationLayer.getChildren().remove(stickerView);
            SoundManager.playSound("fall_sticker.mp3");
            if (onFinish != null) {
                onFinish.accept(null);
            }
        });

        parallelTransition.play();
        SoundManager.playSound("throw_sticker.mp3");
    }

    /**
     * Creates a curved path for the sticker animation.
     * For isFromMe=true: curve upward from bottom-left to top-center of left pane
     * For isFromMe=false: curve downward from top-right to bottom-center of right pane
     */
    private static Path createWavePath(Point2D start, Point2D end, boolean isFromMe) {
        Path path = new Path();
        path.getElements().add(new MoveTo(start.getX(), start.getY()));

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        if (isFromMe) {
            // From bottom-left to top-center: create upward arc
            double midY = Math.min(start.getY(), end.getY()) - Math.abs(dx) * 0.3; // Arc upward
            
            double ctrl1x = start.getX() + dx * 0.25;
            double ctrl1y = start.getY() - Math.abs(dy) * 0.2;
            
            double ctrl2x = start.getX() + dx * 0.75;
            double ctrl2y = midY;
            
            path.getElements().add(new CubicCurveTo(ctrl1x, ctrl1y, ctrl2x, ctrl2y, end.getX(), end.getY()));
        } else {
            // From top-right to bottom-center: create downward arc
            double midY = Math.max(start.getY(), end.getY()) + Math.abs(dx) * 0.3; // Arc downward
            
            double ctrl1x = start.getX() + dx * 0.25;
            double ctrl1y = start.getY() + Math.abs(dy) * 0.2;
            
            double ctrl2x = start.getX() + dx * 0.75;
            double ctrl2y = midY;
            
            path.getElements().add(new CubicCurveTo(ctrl1x, ctrl1y, ctrl2x, ctrl2y, end.getX(), end.getY()));
        }

        return path;
    }
}
