package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.flags.Flags;
import io.quarkiverse.flags.spi.RolloutFlagEvaluator;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifies the username-based gradual rollout of the {@code dashboard.insights-panel} flag: per-user
 * results are stable, and raising the rollout percentage only ever widens the audience (a user
 * enabled at a lower percentage stays enabled at a higher one).
 */
@QuarkusTest
public class InsightsPanelRolloutTest {

    private static final int USERS = 500;

    @Inject
    Flags flags;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @AfterEach
    void restore() {
        // leave the rollout at the seeded percentage for other tests
        setRolloutPercentage(30);
    }

    @ActivateRequestContext
    @Test
    void higherPercentageWidensRolloutMonotonically() {
        List<String> usernames = IntStream.range(0, USERS).mapToObj(i -> "attendee-" + i).toList();

        setRolloutPercentage(10);
        Set<String> enabledAt10 = enabledUsers(usernames);

        setRolloutPercentage(40);
        Set<String> enabledAt40 = enabledUsers(usernames);

        // widening the rollout only adds users, never removes them
        assertTrue(enabledAt40.containsAll(enabledAt10),
                "users enabled at 10% must remain enabled at 40%");
        assertTrue(enabledAt40.size() > enabledAt10.size(),
                "40% should enable more users than 10%");
        // rough sanity with generous bounds to avoid flakiness
        assertTrue(enabledAt10.size() < USERS / 3, "10% rollout should stay well below a third");
        assertTrue(enabledAt40.size() < USERS * 3 / 5, "40% rollout should stay below 60%");
    }

    private Set<String> enabledUsers(List<String> usernames) {
        Set<String> enabled = new HashSet<>();
        for (String username : usernames) {
            identityAssociation.setIdentity(QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .build());
            boolean result = flags.isEnabled(AppInit.INSIGHTS_PANEL, false);
            if (result) {
                enabled.add(username);
            }
            // the same user must get a stable answer across repeated evaluations
            for (int i = 0; i < 5; i++) {
                assertEquals(result, flags.isEnabled(AppInit.INSIGHTS_PANEL, false));
            }
        }
        return enabled;
    }

    private static void setRolloutPercentage(int percentage) {
        QuarkusTransaction.requiringNew().run(() -> {
            DbFlag flag = DbFlag.find("feature", AppInit.INSIGHTS_PANEL).firstResult();
            flag.metadata.put(RolloutFlagEvaluator.ROLLOUT_PERCENTAGE, Integer.toString(percentage));
        });
    }
}
