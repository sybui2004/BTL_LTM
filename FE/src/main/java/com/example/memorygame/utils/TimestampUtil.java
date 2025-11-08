package com.example.memorygame.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for formatting timestamps in chat messages
 */
public class TimestampUtil {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    
    /**
     * Check if timestamp should be displayed for this message
     * Shows timestamp when:
     * - First message in conversation
     * - Messages are more than 5 minutes apart
     * - Different day from previous message
     */
    public static boolean shouldShowTimestamp(LocalDateTime current, LocalDateTime previous) {
        if (previous == null || current == null) {
            return true; // First message or null timestamp
        }
        
        long minutesBetween = ChronoUnit.MINUTES.between(previous, current);
        boolean differentDay = !isSameDay(current, previous);
        
        return minutesBetween > 5 || differentDay;
    }
    
    /**
     * Format timestamp for message display
     * - "HH:mm" if same day
     * - "dd/MM HH:mm" if different day
     */
    public static String formatMessageTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        
        if (isToday(timestamp)) {
            return timestamp.format(TIME_FORMATTER);
        } else {
            return timestamp.format(DATE_TIME_FORMATTER);
        }
    }
    
    /**
     * Format relative time for conversation list
     * - "Vừa xong" (< 1 minute)
     * - "5 phút trước" (< 1 hour) 
     * - "2 giờ trước" (< 24 hours)
     * - "Hôm qua" (1 day ago)
     * - "dd/MM" (> 1 day ago)
     */
    public static String getRelativeTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);
        
        if (minutes < 1) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days == 1) {
            return "Hôm qua";
        } else {
            return timestamp.format(DATE_FORMATTER);
        }
    }
    
    /**
     * Check if two timestamps are on the same day
     */
    public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.toLocalDate().equals(date2.toLocalDate());
    }
    
    /**
     * Check if timestamp is today
     */
    public static boolean isToday(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        return timestamp.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
}