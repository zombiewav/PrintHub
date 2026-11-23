package com.printhub.web;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Lets each phone (or any key) do only so many things in a time window. */
final class RateLimiter {

    private final int maxEvents;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<String, ArrayDeque<Long>> events = new HashMap<>();

    RateLimiter(int maxEvents, long windowMillis, LongSupplier clock) {
        this.maxEvents = maxEvents;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** True and counted if this key is still under the limit; false (and not counted) if it is over. */
    synchronized boolean allow(String key) {
        long now = clock.getAsLong();
        ArrayDeque<Long> times = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst() <= now - windowMillis) {
            times.pollFirst();
        }
        if (times.size() >= maxEvents) {
            return false;
        }
        times.addLast(now);
        return true;
    }
}
