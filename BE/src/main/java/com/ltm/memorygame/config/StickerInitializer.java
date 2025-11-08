package com.ltm.memorygame.config;

import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.model.chat.Sticker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializer để tự động khởi tạo stickers vào database khi app start
 * Tương tự CardInitializer, nhưng cho stickers
 * 
 * Lưu ý: File stickers cần được đổi tên thành sticker_01.png, sticker_02.png, ...
 * trong thư mục src/main/resources/static/stickers/
 */
@Component
@RequiredArgsConstructor
public class StickerInitializer implements CommandLineRunner {
    
    private final StickerRepository stickerRepository;
    
    // Số lượng stickers (19 stickers)
    private static final int STICKER_COUNT = 19;
    
    @Override
    public void run(String... args) throws Exception {
        initializeStickers();
    }
    
    private void initializeStickers() {
        try {
            // Kiểm tra xem đã có stickers với static paths chưa
            List<Sticker> existingStickers = stickerRepository.findAll();
            boolean needsRecreate = false;
            
            // Kiểm tra nếu có sticker nào đang dùng Google Drive URL
            for (Sticker sticker : existingStickers) {
                if (sticker.getSticker_path() != null && 
                    sticker.getSticker_path().contains("drive.google.com")) {
                    needsRecreate = true;
                    System.out.println("[StickerInitializer] Found Google Drive URL, will recreate: " + sticker.getSticker_path());
                    break;
                }
            }
            
            // Kiểm tra số lượng stickers
            if (existingStickers.size() != STICKER_COUNT) {
                needsRecreate = true;
                System.out.println("[StickerInitializer] Sticker count mismatch. Expected: " + STICKER_COUNT + 
                                 ", Found: " + existingStickers.size());
            }
            
            // Kiểm tra xem có sticker nào không dùng static path
            if (!needsRecreate) {
                for (Sticker sticker : existingStickers) {
                    if (sticker.getSticker_path() == null || 
                        !sticker.getSticker_path().startsWith("/static/stickers/")) {
                        needsRecreate = true;
                        System.out.println("[StickerInitializer] Found non-static path: " + sticker.getSticker_path());
                        break;
                    }
                }
            }
            
            if (!needsRecreate) {
                System.out.println("[StickerInitializer] Stickers already initialized with static paths, skipping.");
                return;
            }
            
            System.out.println("[StickerInitializer] Recreating stickers with static paths...");
            
            // Xóa tất cả stickers cũ
            stickerRepository.deleteAll();
            
            // Tạo stickers mới với static paths
            // Format: /static/stickers/sticker_01.png, sticker_02.png, ...
            for (int i = 1; i <= STICKER_COUNT; i++) {
                Sticker sticker = new Sticker();
                String fileName = String.format("sticker_%02d.png", i);
                sticker.setSticker_path("/static/stickers/" + fileName);
                stickerRepository.save(sticker);
                System.out.println("[StickerInitializer] Created sticker: " + sticker.getSticker_path());
            }
            
            System.out.println("[StickerInitializer] Initialized " + stickerRepository.count() + " stickers with static paths.");
            System.out.println("[StickerInitializer] Note: Make sure sticker files are renamed to sticker_01.png through sticker_19.png");
            
        } catch (Exception e) {
            System.err.println("[StickerInitializer] Error initializing stickers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

