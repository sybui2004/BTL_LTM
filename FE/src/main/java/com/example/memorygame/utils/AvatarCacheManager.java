package com.example.memorygame.utils;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Singleton cache manager for user avatars to improve performance
 */
public class AvatarCacheManager {
    private static volatile AvatarCacheManager instance;
    private final Map<String, Image> avatarCache;
    private final Image defaultAvatar;
    
    private AvatarCacheManager() {
        this.avatarCache = new ConcurrentHashMap<>();
        
        // Load default avatar once - try multiple paths
        Image defaultImg = null;
        String[] possiblePaths = {
            "/com/example/memorygame/assets/images/default_avatar.png",
            "/com/example/memorygame/assets/images/avatar/avatar2.png", 
            "/com/example/memorygame/assets/images/avt1.png",
            "/com/example/memorygame/assets/images/name.png",
            "/assets/images/name.png",
            "/images/name.png"
        };
        
        for (String path : possiblePaths) {
            try {
                java.io.InputStream stream = getClass().getResourceAsStream(path);
                if (stream != null) {
                    defaultImg = new Image(stream);
                    System.out.println("[AvatarCache] Loaded default avatar from: " + path);
                    break;
                }
            } catch (Exception e) {
                System.err.println("[AvatarCache] Failed to load default avatar from " + path + ": " + e.getMessage());
            }
        }
        
        // If no default avatar found, create a simple placeholder
        if (defaultImg == null) {
            System.err.println("[AvatarCache] No default avatar found, creating placeholder");
            // Create a simple 44x44 colored rectangle as fallback
            try {
                javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(44, 44);
                javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                gc.fillOval(0, 0, 44, 44);
                javafx.scene.image.WritableImage placeholder = canvas.snapshot(null, null);
                defaultImg = placeholder;
            } catch (Exception e) {
                System.err.println("[AvatarCache] Failed to create placeholder: " + e.getMessage());
            }
        }
        
        this.defaultAvatar = defaultImg;
        System.out.println("[AvatarCache] Initialized with default avatar: " + (defaultAvatar != null));
    }
    
    public static AvatarCacheManager getInstance() {
        if (instance == null) {
            synchronized (AvatarCacheManager.class) {
                if (instance == null) {
                    instance = new AvatarCacheManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get cached avatar or load it if not cached
     * Returns immediately with default avatar if not cached, then loads in background
     */
    public Image getAvatar(String avatarUrl) {
        System.out.println("[AvatarCache] getAvatar called with: " + avatarUrl);
        
        if (avatarUrl == null || avatarUrl.isEmpty() || "null".equals(avatarUrl)) {
            System.out.println("[AvatarCache] Invalid avatar URL, returning default");
            return defaultAvatar;
        }
        
        // Check cache first
        Image cachedImage = avatarCache.get(avatarUrl);
        if (cachedImage != null) {
            System.out.println("[AvatarCache] Using cached avatar for: " + avatarUrl);
            return cachedImage;
        }
        
        // Build full URL
        String imageUrl = buildFullAvatarUrl(avatarUrl);
        System.out.println("[AvatarCache] Built full URL: " + imageUrl);
        
        // Check cache with full URL
        cachedImage = avatarCache.get(imageUrl);
        if (cachedImage != null) {
            System.out.println("[AvatarCache] Using cached avatar for full URL: " + imageUrl);
            return cachedImage;
        }
        
        // Start loading in background but return image with loading state
        System.out.println("[AvatarCache] Starting background load for: " + imageUrl);
        Image loadingImage = loadAvatarAsync(avatarUrl, imageUrl);
        
        // Return loading image (or default if failed to start loading)
        return loadingImage != null ? loadingImage : defaultAvatar;
    }
    
    /**
     * Load avatar asynchronously and cache it
     */
    private Image loadAvatarAsync(String originalUrl, String fullUrl) {
        try {
            System.out.println("[AvatarCache] Creating Image with URL: " + fullUrl);
            Image image = new Image(fullUrl, true); // background loading
            
            // Cache the image when it's loaded or failed
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                System.out.println("[AvatarCache] Progress for " + originalUrl + ": " + newProgress);
                if (newProgress.doubleValue() >= 1.0) {
                    if (!image.isError()) {
                        avatarCache.put(originalUrl, image);
                        avatarCache.put(fullUrl, image);
                        System.out.println("[AvatarCache] Successfully cached avatar: " + originalUrl);
                    } else {
                        System.err.println("[AvatarCache] Failed to load avatar: " + fullUrl + " - " + image.getException());
                        // Cache default avatar to avoid repeated failed attempts
                        if (defaultAvatar != null) {
                            avatarCache.put(originalUrl, defaultAvatar);
                            avatarCache.put(fullUrl, defaultAvatar);
                        }
                    }
                }
            });
            
            // Also add error listener
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    System.err.println("[AvatarCache] Error loading image: " + fullUrl);
                }
            });
            
            return image;
        } catch (Exception e) {
            System.err.println("[AvatarCache] Error starting avatar load: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get avatar with callback when loaded (for immediate UI updates)
     */
    public Image getAvatarWithCallback(String avatarUrl, Runnable onLoaded) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return defaultAvatar;
        }
        
        // Check cache first
        Image cachedImage = avatarCache.get(avatarUrl);
        if (cachedImage != null) {
            return cachedImage;
        }
        
        String imageUrl = buildFullAvatarUrl(avatarUrl);
        cachedImage = avatarCache.get(imageUrl);
        if (cachedImage != null) {
            return cachedImage;
        }
        
        // Start loading with callback
        try {
            Image image = new Image(imageUrl, true);
            
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0) {
                    if (!image.isError()) {
                        avatarCache.put(avatarUrl, image);
                        avatarCache.put(imageUrl, image);
                        if (onLoaded != null) {
                            javafx.application.Platform.runLater(onLoaded);
                        }
                    }
                }
            });
            
            return image;
        } catch (Exception e) {
            System.err.println("[AvatarCache] Error loading avatar with callback: " + e.getMessage());
            return defaultAvatar;
        }
    }
    
    /**
     * Get default avatar
     */
    public Image getDefaultAvatar() {
        return defaultAvatar;
    }
    
    /**
     * Build full avatar URL from relative path
     */
    private String buildFullAvatarUrl(String avatarUrl) {
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            return avatarUrl;
        }
        
        if (avatarUrl.startsWith("/static/")) {
            return "http://localhost:8080" + avatarUrl;
        } else if (!avatarUrl.startsWith("/")) {
            return "http://localhost:8080/static/avatars/" + avatarUrl;
        } else {
            return "http://localhost:8080" + avatarUrl;
        }
    }
    
    /**
     * Pre-load avatar (for conversation lists, etc.)
     */
    public void preloadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty() && !avatarCache.containsKey(avatarUrl)) {
            getAvatar(avatarUrl); // This will start loading in background
        }
    }
    
    /**
     * Clear cache (for memory management)
     */
    public void clearCache() {
        avatarCache.clear();
        System.out.println("[AvatarCache] Cache cleared");
    }
    
    /**
     * Get cache size for debugging
     */
    public int getCacheSize() {
        return avatarCache.size();
    }
}