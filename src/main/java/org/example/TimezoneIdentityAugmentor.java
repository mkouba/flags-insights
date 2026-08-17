package org.example;

import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Attaches the logged-in user's {@link java.time.ZoneId time zone} to the {@link SecurityIdentity}
 * as an attribute so that downstream code (e.g. {@link ThemeFlagEvaluator}) can read it without
 * hitting the database on the request thread.
 */
@ApplicationScoped
public class TimezoneIdentityAugmentor implements SecurityIdentityAugmentor {

    public static final String TIMEZONE_ATTR = "timezone";

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        return context.runBlocking(() -> {
            String username = identity.getPrincipal().getName();
            ZoneId timezone = QuarkusTransaction.requiringNew().call(() -> {
                User user = User.find("username", username).firstResult();
                return user == null ? null : user.timezone;
            });
            if (timezone == null) {
                return identity;
            }
            return QuarkusSecurityIdentity.builder(identity)
                    .addAttribute(TIMEZONE_ATTR, timezone)
                    .build();
        });
    }
}
