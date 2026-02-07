package org.example.sharedprompts.global.util;

public class DurationFormatter {
    
    public static String formatDowntime(long durationMs) {
        if (durationMs <= 0) {
            return "알 수 없음";
        }
        
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d일 %d시간 %d분", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d시간 %d분", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds % 60);
        } else {
            return String.format("%d초", seconds);
        }
    }
}

