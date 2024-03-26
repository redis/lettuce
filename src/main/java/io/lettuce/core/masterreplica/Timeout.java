package io.lettuce.core.masterreplica;

import java.util.concurrent.TimeUnit;

/**
 * Value object to represent a timeout.
 *
 * @author Mark Paluch
 * @since 4.2
 */
class Timeout {

    private final long expiresMs;

    public Timeout(long timeout, TimeUnit timeUnit) {
        this.expiresMs = System.currentTimeMillis() + timeUnit.toMillis(timeout);
    }

    public boolean isExpired() {
        return expiresMs < System.currentTimeMillis();
    }

    public long remaining() {

        long diff = expiresMs - System.currentTimeMillis();
        if (diff > 0) {
            return diff;
        }
        return 0;
    }

}
