package at.rtr.rmbt.map.service;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarkerServiceTimeStringTest {

    // 2026-09-26T08:49:21.137Z
    private static final Date TIME = new Date(1790412561137L);

    // DateFormat MEDIUM may use a narrow no-break space before AM/PM depending on the JDK
    private static String normalize(String s) {
        return s.replace(' ', ' ');
    }

    @Test
    void formatsInTimeZoneOfMeasuringClient() {
        assertEquals("Sep 26, 2026, 10:49:21 AM",
                normalize(MarkerService.formatTimeString(TIME, "Europe/Vienna", Locale.US)));
    }

    @Test
    void nullTimeZoneFallsBackToUtc() {
        assertEquals("Sep 26, 2026, 8:49:21 AM",
                normalize(MarkerService.formatTimeString(TIME, null, Locale.US)));
    }

    @Test
    void blankTimeZoneFallsBackToUtc() {
        assertEquals("Sep 26, 2026, 8:49:21 AM",
                normalize(MarkerService.formatTimeString(TIME, " ", Locale.US)));
    }

    @Test
    void unknownTimeZoneFallsBackToUtc() {
        assertEquals("Sep 26, 2026, 8:49:21 AM",
                normalize(MarkerService.formatTimeString(TIME, "CEST", Locale.US)));
    }

    @Test
    void nullTimeYieldsNull() {
        assertNull(MarkerService.formatTimeString(null, "Europe/Vienna", Locale.US));
    }
}
