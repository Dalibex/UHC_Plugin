package me.dalibex.UHC_DBasic.utils;

/**
 * Utilidad para el formateo de duraciones.
 * Centraliza los formatos de tiempo del plugin para evitar duplicarlos.
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Formatea segundos como reloj: "MM:SS" o "HH:MM:SS" cuando supera la hora.
     */
    public static String formatClock(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int sec = totalSeconds % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    /**
     * Formatea segundos como "HHh MMm SSs" (siempre con dos dígitos por unidad).
     */
    public static String formatHms(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int sec = totalSeconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, sec);
    }
}