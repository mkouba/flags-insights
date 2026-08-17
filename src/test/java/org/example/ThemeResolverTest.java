package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

public class ThemeResolverTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
    private static final ZoneId AUCKLAND = ZoneId.of("Pacific/Auckland");

    @Test
    void nullZoneIsLight() {
        assertEquals("light", ThemeFlagEvaluator.resolveTheme(null, Instant.now()));
    }

    @Test
    void nightIsDark() {
        // 22:00 in Tokyo -> dark
        Instant tokyoNight = ZonedDateTime.of(2026, 1, 15, 22, 0, 0, 0, TOKYO).toInstant();
        assertEquals("dark", ThemeFlagEvaluator.resolveTheme(TOKYO, tokyoNight));
    }

    @Test
    void daytimeIsLight() {
        // The same instant that is 22:00 in Tokyo is 08:00 in New York -> light
        Instant tokyoNight = ZonedDateTime.of(2026, 1, 15, 22, 0, 0, 0, TOKYO).toInstant();
        assertEquals("light", ThemeFlagEvaluator.resolveTheme(NEW_YORK, tokyoNight));
    }

    @Test
    void nightWindowBoundaries() {
        // 21:00 -> dark (inclusive lower edge of the night window)
        assertEquals("dark", themeAt(21, 0));
        // 05:59 -> dark (still within the night window)
        assertEquals("dark", themeAt(5, 59));
        // 06:00 -> light (night window is exclusive at 06:00)
        assertEquals("light", themeAt(6, 0));
        // 20:59 -> light (just before the night window)
        assertEquals("light", themeAt(20, 59));
    }

    @Test
    void aucklandIsDarkWhilePragueIsLight() {
        // 14:00 in Prague (CEST, summer) is 00:00 the next day in Auckland (NZST): kiri (Auckland)
        // gets the dark theme at the very moment alice (Prague) gets the light theme.
        Instant praguesAfternoon = ZonedDateTime.of(2026, 8, 17, 14, 0, 0, 0, PRAGUE).toInstant();
        assertEquals("light", ThemeFlagEvaluator.resolveTheme(PRAGUE, praguesAfternoon));
        assertEquals("dark", ThemeFlagEvaluator.resolveTheme(AUCKLAND, praguesAfternoon));
    }

    private static String themeAt(int hour, int minute) {
        Instant instant = ZonedDateTime.of(2026, 1, 15, hour, minute, 0, 0, TOKYO).toInstant();
        return ThemeFlagEvaluator.resolveTheme(TOKYO, instant);
    }
}
