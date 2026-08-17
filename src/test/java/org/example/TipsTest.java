package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Tips#pick(int)}: the sample size is honored and clamped, and the tips are
 * distinct and drawn from the pool.
 */
public class TipsTest {

    @Test
    void picksRequestedNumberOfDistinctTips() {
        List<String> tips = Tips.pick(3);
        assertEquals(3, tips.size());
        assertEquals(3, new HashSet<>(tips).size(), "tips must be distinct");
        assertTrue(Tips.POOL.containsAll(tips), "tips must come from the pool");
    }

    @Test
    void clampsToPoolSize() {
        List<String> tips = Tips.pick(Tips.POOL.size() + 100);
        assertEquals(Tips.POOL.size(), tips.size());
        assertTrue(tips.containsAll(Tips.POOL), "clamping to the pool size returns every tip");
    }

    @Test
    void nonPositiveCountReturnsEmpty() {
        assertTrue(Tips.pick(0).isEmpty());
        assertTrue(Tips.pick(-5).isEmpty());
    }
}
