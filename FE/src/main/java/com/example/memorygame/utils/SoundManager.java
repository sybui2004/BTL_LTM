package com.example.memorygame.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import javax.sound.sampled.*;
import java.io.IOException;

/**
 * Utility class for managing game sounds and music
 * 
 * IMPORTANT: To enable sound, you need to add these VM options when running:
 *   --add-opens javafx.base/com.sun.javafx=javafx.media
 *   --add-opens javafx.base/com.sun.javafx=javafx.graphics
 *   --add-opens javafx.graphics/com.sun.javafx.application=javafx.media
 *   --add-opens javafx.graphics/com.sun.javafx.tk=javafx.media
 *   --add-opens javafx.media/com.sun.media.jfxmedia=ALL-UNNAMED
 *   --add-opens javafx.media/com.sun.media.jfxmedia.events=ALL-UNNAMED
 *   --add-opens javafx.media/com.sun.media.jfxmedia.locator=ALL-UNNAMED
 * 
 * See README_SOUND_SETUP.md for detailed instructions.
 */
public class SoundManager {
    private static MediaPlayer backgroundMusicPlayer;
    private static Clip backgroundClip;
    private static final String SOUND_PATH = "/com/example/memorygame/assets/sound/";
    private static boolean useJavaxFallback = false; // Switch to javax.sound on JavaFX media issues
    private static boolean soundDisabled = false; // Disable completely if even fallback fails
    private static String currentMusicFile = null; // Track current playing music file
    
    /**
     * Play background music (looping)
     * If the same music file is already playing, it will continue playing without restarting
     * @param musicFile Name of the music file (e.g., "game_music_loop.wav")
     */
    public static void playBackgroundMusic(String musicFile) {
        if (soundDisabled) {
            return;
        }
        
        // If the same music is already playing, don't restart it
        if (currentMusicFile != null && currentMusicFile.equals(musicFile)) {
            boolean isPlaying = false;
            if (useJavaxFallback) {
                isPlaying = (backgroundClip != null && backgroundClip.isRunning());
            } else {
                isPlaying = (backgroundMusicPlayer != null && 
                            backgroundMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING);
            }
            if (isPlaying) {
                System.out.println("[SoundManager] Music already playing: " + musicFile + ", skipping restart");
                return;
            }
        }
        
        stopBackgroundMusic();
        currentMusicFile = musicFile;
        if (useJavaxFallback) {
            playBackgroundMusicJavax(musicFile);
            return;
        }
        try {
            URL resource = SoundManager.class.getResource(SOUND_PATH + musicFile);
            if (resource == null) {
                System.err.println("[SoundManager] Cannot find music file: " + musicFile);
                return;
            }
            String mediaUrl = resource.toExternalForm();
            Media media = new Media(mediaUrl);
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.setVolume(0.5);
            backgroundMusicPlayer.setOnError(() -> {
                System.err.println("[SoundManager] MediaPlayer error: " + backgroundMusicPlayer.getError());
                trySwitchToJavaxFallbackAndPlayBg(musicFile);
            });
            backgroundMusicPlayer.play();
            System.out.println("[SoundManager] Playing background music (JavaFX): " + musicFile);
        } catch (NoSuchMethodError | IllegalAccessError | Exception e) {
            System.err.println("[SoundManager] JavaFX media error: " + e.getClass().getSimpleName() + ", switching to javax.sound fallback");
            trySwitchToJavaxFallbackAndPlayBg(musicFile);
        }
    }

    private static void trySwitchToJavaxFallbackAndPlayBg(String musicFile) {
        useJavaxFallback = true;
        playBackgroundMusicJavax(musicFile);
    }

    private static void playBackgroundMusicJavax(String musicFile) {
        try {
            URL url = SoundManager.class.getResource(SOUND_PATH + musicFile);
            if (url == null) {
                System.err.println("[SoundManager] Cannot find music file: " + musicFile);
                return;
            }
            stopBackgroundMusic();
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioInputStream);
            // Try set volume
            try {
                FloatControl volume = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(linearToDecibel(0.5f));
            } catch (IllegalArgumentException ignored) {}
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
            System.out.println("[SoundManager] Playing background music (javax.sound): " + musicFile);
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            soundDisabled = true;
            System.err.println("[SoundManager] Fallback audio failed, sound disabled: " + ex.getClass().getSimpleName());
        }
    }
    
    /**
     * Stop background music
     */
    public static void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.dispose();
            backgroundMusicPlayer = null;
        }
        if (backgroundClip != null) {
            backgroundClip.stop();
            backgroundClip.close();
            backgroundClip = null;
        }
        currentMusicFile = null; // Reset current music file when stopped
    }
    
    /**
     * Play a sound effect (one-time playback)
     * @param soundFile Name of the sound file (e.g., "button.wav")
     */
    public static void playSound(String soundFile) {
        if (soundDisabled) {
            return;
        }
        if (useJavaxFallback) {
            playSoundJavax(soundFile);
            return;
        }
        try {
            URL resource = SoundManager.class.getResource(SOUND_PATH + soundFile);
            if (resource == null) {
                System.err.println("[SoundManager] Cannot find sound file: " + soundFile);
                return;
            }
            String mediaUrl = resource.toExternalForm();
            Media media = new Media(mediaUrl);
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.7);
            mediaPlayer.setOnError(() -> {
                System.err.println("[SoundManager] MediaPlayer error: " + mediaPlayer.getError());
                mediaPlayer.dispose();
                useJavaxFallback = true;
                playSoundJavax(soundFile);
            });
            mediaPlayer.setOnEndOfMedia(mediaPlayer::dispose);
            mediaPlayer.play();
        } catch (NoSuchMethodError | IllegalAccessError | Exception e) {
            System.err.println("[SoundManager] JavaFX media error: " + e.getClass().getSimpleName() + ", switching to javax.sound fallback");
            useJavaxFallback = true;
            playSoundJavax(soundFile);
        }
    }

    private static void playSoundJavax(String soundFile) {
        try {
            URL url = SoundManager.class.getResource(SOUND_PATH + soundFile);
            if (url == null) {
                System.err.println("[SoundManager] Cannot find sound file: " + soundFile);
                return;
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            try {
                FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(linearToDecibel(0.7f));
            } catch (IllegalArgumentException ignored) {}
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            soundDisabled = true;
            System.err.println("[SoundManager] Fallback audio failed, sound disabled: " + ex.getClass().getSimpleName());
        }
    }
    
    /**
     * Set background music volume (0.0 to 1.0)
     */
    public static void setBackgroundMusicVolume(double volume) {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
        }
    }
    
    /**
     * Check if background music is playing
     */
    public static boolean isBackgroundMusicPlaying() {
        boolean fxPlaying = backgroundMusicPlayer != null &&
               backgroundMusicPlayer.getStatus() == MediaPlayer.Status.PLAYING;
        boolean clipPlaying = backgroundClip != null && backgroundClip.isRunning();
        return fxPlaying || clipPlaying;
    }
    
    /**
     * Check if sound is enabled
     */
    public static boolean isSoundEnabled() {
        return !soundDisabled;
    }
    
    /**
     * Manually enable/disable sound (useful for testing or user preferences)
     */
    public static void setSoundEnabled(boolean enabled) {
        soundDisabled = !enabled;
        if (!enabled) {
            stopBackgroundMusic();
        }
    }

    private static float linearToDecibel(float linear) {
        // Clamp [0,1]
        float cl = Math.max(0.0001f, Math.min(1f, linear));
        return (float) (20.0 * Math.log10(cl));
    }
}

