package me.dalibex.UHC_DBasic.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de formateo de tiempos (lógica pura).
 */
class TimeUtilTest {

    @Test
    void formatClock_underHour_usesMMSS() {
        assertEquals("00:00", TimeUtil.formatClock(0));
        assertEquals("00:59", TimeUtil.formatClock(59));
        assertEquals("01:00", TimeUtil.formatClock(60));
        assertEquals("59:59", TimeUtil.formatClock(3599));
    }

    @Test
    void formatClock_fromHour_usesHHMMSS() {
        assertEquals("01:00:00", TimeUtil.formatClock(3600));
        assertEquals("02:02:05", TimeUtil.formatClock(7325));
        assertEquals("20:05:00", TimeUtil.formatClock(20 * 3600 + 5 * 60));
    }

    @Test
    void formatHms_directHms() {
        assertEquals("00h 00m 00s", TimeUtil.formatHms(0));
        assertEquals("00h 01m 40s", TimeUtil.formatHms(100));
        assertEquals("01h 01m 40s", TimeUtil.formatHms(3700));
        assertEquals("25h 00m 00s", TimeUtil.formatHms(25 * 3600));
    }
}