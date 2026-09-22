package com.sangluo.onestep.feature.embedding;

/**
 * Rate-limits a restore/restart action that the platform can keep re-triggering.
 * Without it, the HOME-restore handshake with MIUI's own launcher can oscillate
 * hundreds of times per minute and never settle on either side.
 *
 * <p>Allows up to {@code limit} actions per {@code windowMs}; exceeding the budget
 * opens the circuit for {@code cooldownMs}. All times come from the caller so the
 * breaker stays unit-testable on the JVM.
 */
public final class RestoreCircuitBreaker {
    private final int limit;
    private final long windowMs;
    private final long cooldownMs;

    private int count;
    private long windowStart;
    private long openUntil;

    public RestoreCircuitBreaker(int limit, long windowMs, long cooldownMs) {
        this.limit = Math.max(1, limit);
        this.windowMs = Math.max(1, windowMs);
        this.cooldownMs = Math.max(0, cooldownMs);
    }

    /** Returns true when the action is allowed now; false while tripped. */
    public synchronized boolean tryAcquire(long now) {
        if (now < openUntil) {
            return false;
        }
        if (now - windowStart > windowMs) {
            windowStart = now;
            count = 0;
        }
        count++;
        if (count > limit) {
            openUntil = now + cooldownMs;
            count = 0;
            return false;
        }
        return true;
    }

    public synchronized boolean isTripped(long now) {
        return now < openUntil;
    }
}
