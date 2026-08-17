package org.example;

import io.quarkiverse.flags.RegisterFlag;

/**
 * Declares the {@code dashboard.tips-shown} feature flag from a mutable static field, demonstrating
 * the in-memory {@code @RegisterFlag} source: reading the field returns the current flag value (the
 * read is rewritten at build time to compute the flag) and assigning it changes the value at runtime.
 * The field is {@code volatile} so writes are visible across threads immediately - the in-memory
 * source is not cached, so no cache invalidation is needed.
 */
public class TipsFlag {

    public static final String TIPS_SHOWN = "dashboard.tips-shown";

    @RegisterFlag(name = TIPS_SHOWN)
    public static volatile int tipsShown = 3;
}
