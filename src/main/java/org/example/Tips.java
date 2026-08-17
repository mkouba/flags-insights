package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fixed, in-memory pool of tips about the quarkus-flags extension. The dashboard shows a random
 * sample of them; the sample size is driven by the {@link TipsFlag#TIPS_SHOWN} feature flag.
 */
public final class Tips {

    private Tips() {
    }

    static final List<String> POOL = List.of(
            "Feature flags can come from many sources: config properties, the database, or in-memory @RegisterFlag fields.",
            "@RegisterFlag turns a static field into a feature flag - reading the field returns the current value.",
            "Implement a custom FlagEvaluator to compute a flag's value dynamically on every request.",
            "Store flags in the database with @FlagSource to flip them at runtime, no redeploy required.",
            "Attach metadata like rollout-percentage to drive a gradual, username-based rollout.",
            "The UsernameRolloutFlagEvaluator hashes username + feature for a stable per-user rollout.",
            "Read flags straight from Qute templates with the {flag:enabled('...')} namespace.",
            "Define config flags under quarkus.flags.build (fixed) or quarkus.flags.runtime (overridable).",
            "Enable the flags cache in production and tune its TTL; disable it in tests for instant changes.",
            "Invalidate the FlagCache after changing a cached flag to make the change visible immediately.");

    /**
     * Returns up to {@code count} distinct tips picked at random from the pool. The count is clamped
     * to the range {@code [0, POOL.size()]}.
     */
    static List<String> pick(int count) {
        int size = Math.max(0, Math.min(count, POOL.size()));
        List<String> shuffled = new ArrayList<>(POOL);
        Collections.shuffle(shuffled);
        return List.copyOf(shuffled.subList(0, size));
    }
}
