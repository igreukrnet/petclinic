package org.springframework.samples.petclinic.test.ai;

import java.util.concurrent.atomic.AtomicInteger;

/** Counts AI triage calls and refuses them once the limit is reached. Thread-safe. */
final class TriageBudget {

    private final int limit;
    private final AtomicInteger used = new AtomicInteger();

    TriageBudget(int limit) {
        this.limit = Math.max(0, limit);
    }

    /** Takes one call from the budget. Returns {@code false} when none is left. */
    boolean tryAcquire() {
        return used.getAndUpdate(n -> n < limit ? n + 1 : n) < limit;
    }

    int limit() {
        return limit;
    }
}
