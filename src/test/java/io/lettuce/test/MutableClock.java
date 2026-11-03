package io.lettuce.test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {

    AtomicReference<Instant> currentTime = new AtomicReference<>(Instant.now());

    public MutableClock() {
        this.currentTime.set(Instant.now());
    }

    public MutableClock(Instant startTime) {
        this.currentTime.set(startTime);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return currentTime.get();
    }

    public void tick(Duration duration) {
        currentTime.set(currentTime.get().plus(duration));
    }

};
