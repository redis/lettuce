package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

/**
 * {@link Delay} with a constant delay for each attempt.
 * 
 * @author Mark Paluch
 */
class ConstantDelay extends Delay {

    private final long delay;

    ConstantDelay(long delay, TimeUnit timeUnit) {

        super(timeUnit);
        this.delay = delay;
    }

    @Override
    public long createDelay(long attempt) {
        return delay;
    }
}
