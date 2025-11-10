package com.ltm.memorygame.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.StickerType;

import lombok.RequiredArgsConstructor;

/**
 * Initializer để tự động khởi tạo stickers vào database khi app start.
 * Sẽ khởi tạo 2 loại sticker: NORMAL và MATCH.
 */
@Component
@RequiredArgsConstructor
public class StickerInitializer implements CommandLineRunner {
    
    private final StickerRepository stickerRepository;
    
    // Số lượng stickers cho mỗi loại
    private static final int NORMAL_STICKER_COUNT = 19;
    private static final int MATCH_STICKER_COUNT = 19; // Dựa trên số file đã đổi tên
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Khởi tạo sticker loại NORMAL
        initializeStickerType(
            StickerType.NORMAL,
            NORMAL_STICKER_COUNT,
            "/static/stickers/",
            "sticker_%02d.png"
        );
        
        // Khởi tạo sticker loại MATCH
        initializeStickerType(
            StickerType.MATCH,
            MATCH_STICKER_COUNT,
            "/static/sticker_match/",
            "sticker_match_%02d.png"
        );
    }
    
    private void initializeStickerType(StickerType type, int expectedCount, String pathPrefix, String fileNameFormat) {
        System.out.println("[StickerInitializer] Checking for sticker type: " + type);
        
        List<Sticker> existingStickers = stickerRepository.findByType(type);
        boolean needsRecreate = false;
        
        // 1. Kiểm tra số lượng
        if (existingStickers.size() != expectedCount) {
            needsRecreate = true;
            System.out.println("[StickerInitializer] " + type + " sticker count mismatch. Expected: " + expectedCount + 
                             ", Found: " + existingStickers.size());
        }
        
        // 2. Kiểm tra đường dẫn
        if (!needsRecreate) {
            for (Sticker sticker : existingStickers) {
                if (sticker.getStickerPath() == null || !sticker.getStickerPath().startsWith(pathPrefix)) {
                    needsRecreate = true;
                    System.out.println("[StickerInitializer] Found incorrect path for " + type + " type: " + sticker.getStickerPath());
                    break;
                }
            }
        }
        
        if (!needsRecreate) {
            System.out.println("[StickerInitializer] " + type + " stickers are already initialized correctly.");
            return;
        }
        
        System.out.println("[StickerInitializer] Recreating " + type + " stickers...");
        
        // Xóa các sticker cũ của loại này
        if (!existingStickers.isEmpty()) {
            stickerRepository.deleteAll(existingStickers);
            System.out.println("[StickerInitializer] Deleted " + existingStickers.size() + " old " + type + " stickers.");
        }
        
        // Tạo stickers mới
        for (int i = 1; i <= expectedCount; i++) {
            Sticker sticker = new Sticker();
            String fileName = String.format(fileNameFormat, i);
            sticker.setStickerPath(pathPrefix + fileName);
            sticker.setType(type);
            stickerRepository.save(sticker);
        }
        
        System.out.println("[StickerInitializer] Initialized " + expectedCount + " " + type + " stickers.");
    }
}

