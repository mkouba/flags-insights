package org.example;

import java.time.Instant;
import java.time.ZoneId;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.flags.Flag;
import io.quarkiverse.flags.Flag.ComputationContext;
import io.quarkiverse.flags.Flag.Value;
import io.quarkiverse.flags.StringValue;
import io.quarkiverse.flags.spi.FlagEvaluator;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Computes the {@code theme} flag based on the current time in the logged-in user's time zone.
 * <p>
 * The time zone is read from the {@link SecurityIdentity} attribute set by
 * {@link TimezoneIdentityAugmentor}, so no database access happens during evaluation. If it is
 * night ({@value #NIGHT_FROM_HOUR}:00 up to {@value #NIGHT_TO_HOUR}:00) the flag evaluates to
 * {@value #DARK}, otherwise to {@value #LIGHT}. Anonymous users get {@value #LIGHT}.
 */
@Identifier(ThemeFlagEvaluator.ID)
@Singleton
public class ThemeFlagEvaluator implements FlagEvaluator {

    public static final String ID = "insights.theme";

    static final String LIGHT = "light";
    static final String DARK = "dark";

    static final int NIGHT_FROM_HOUR = 21;
    static final int NIGHT_TO_HOUR = 6;

    @Inject
    SecurityIdentity identity;

    @Override
    public Uni<Value> evaluate(Flag flag, Value initialValue, ComputationContext computationContext) {
        ZoneId zone = identity.getAttribute(TimezoneIdentityAugmentor.TIMEZONE_ATTR);
        return StringValue.createUni(resolveTheme(zone, Instant.now()));
    }

    // package-private + static so it can be unit-tested without Quarkus
    static String resolveTheme(ZoneId zone, Instant now) {
        if (zone == null) {
            return LIGHT;
        }
        int hour = now.atZone(zone).getHour();
        return (hour >= NIGHT_FROM_HOUR || hour < NIGHT_TO_HOUR) ? DARK : LIGHT;
    }
}
