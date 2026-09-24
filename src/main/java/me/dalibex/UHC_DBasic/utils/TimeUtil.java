package me.dalibex.UHC_DBasic.utils;

/** Centralized duration formatting helpers. */
public final class TimeUtil {

    private TimeUtil() {
    }

    /** Formats seconds as "MM:SS" or "HH:MM:SS" when at least one hour. */
    public static String formatClock(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int sec = totalSeconds % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    /** Formats seconds as "HHh MMm SSs" with two digits per unit. */
    public static String formatHms(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int sec = totalSeconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, sec);
    }
}
