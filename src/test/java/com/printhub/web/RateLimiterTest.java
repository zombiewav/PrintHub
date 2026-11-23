package com.printhub.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsTheLimitThenRefusesUntilTheWindowPasses() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(3, 10_000, now::get);
        assertTrue(limiter.allow("phone-1"));
        assertTrue(limiter.allow("phone-1"));
        assertTrue(limiter.allow("phone-1"));
        assertFalse(limiter.allow("phone-1"));
        now.set(10_001);
        assertTrue(limiter.allow("phone-1"));
    }

    @Test
    void eachPhoneHasItsOwnAllowance() {
        RateLimiter limiter = new RateLimiter(1, 10_000, () -> 0L);
        assertTrue(limiter.allow("phone-1"));
        assertFalse(limiter.allow("phone-1"));
        assertTrue(limiter.allow("phone-2"));
    }

    @Test
    void oldEventsSlideOutOfTheWindowOneByOne() {
        AtomicLong now = new AtomicLong(0);
        RateLimiter limiter = new RateLimiter(2, 10_000, now::get);
        assertTrue(limiter.allow("p"));
        now.set(6_000);
        assertTrue(limiter.allow("p"));
        assertFalse(limiter.allow("p"));
        now.set(10_500); // the first event is now older than the window, the second is not
        assertTrue(limiter.allow("p"));
        assertFalse(limiter.allow("p"));
    }
}
