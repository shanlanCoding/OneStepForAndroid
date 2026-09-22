package com.sangluo.onestep.feature.embedding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Validates the oscillation guard used by the HOME restore and restart loops. */
public class RestoreCircuitBreakerTest {

    @Test
    public void allowsUpToLimitInsideWindow() {
        RestoreCircuitBreaker breaker = new RestoreCircuitBreaker(3, 100, 50);
        assertTrue(breaker.tryAcquire(0));
        assertTrue(breaker.tryAcquire(10));
        assertTrue(breaker.tryAcquire(20));
    }

    @Test
    public void tripsAfterLimitAndRecoversAfterCooldown() {
        RestoreCircuitBreaker breaker = new RestoreCircuitBreaker(2, 100, 50);
        assertTrue(breaker.tryAcquire(0));
        assertTrue(breaker.tryAcquire(1));
        assertFalse(breaker.tryAcquire(2));
        assertFalse(breaker.tryAcquire(51));
        assertTrue(breaker.tryAcquire(51 + 50));
    }

    @Test
    public void windowResetRestoresBudget() {
        RestoreCircuitBreaker breaker = new RestoreCircuitBreaker(2, 100, 50);
        assertTrue(breaker.tryAcquire(0));
        assertTrue(breaker.tryAcquire(1));
        assertTrue(breaker.tryAcquire(200));
        assertTrue(breaker.tryAcquire(201));
    }

    @Test
    public void trippedStateReflectsCooldownWindow() {
        RestoreCircuitBreaker breaker = new RestoreCircuitBreaker(1, 100, 40);
        assertTrue(breaker.tryAcquire(10));
        assertFalse(breaker.tryAcquire(11));
        assertTrue(breaker.isTripped(12));
        assertFalse(breaker.isTripped(52));
        assertTrue(breaker.tryAcquire(52));
    }
}
