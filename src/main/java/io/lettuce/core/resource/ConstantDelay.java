package io.lettuce.core.resource;

import java.time.Duration;

/**
 * {@link Delay} with a constant delay for each attempt.
 *
 * @author Mark Paluch
 */
class ConstantDelay extends Delay {

    private final Duration delay;

    ConstantDelay(Duration delay) {
        this.delay = delay;
    }

    @Override
    public Duration createDelay(long attempt) {
        return delay;
    }

}
