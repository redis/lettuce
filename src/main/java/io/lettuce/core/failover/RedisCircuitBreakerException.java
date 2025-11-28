package io.lettuce.core.failover;

import io.lettuce.core.RedisException;

public class RedisCircuitBreakerException extends RedisException {

    public static final RedisCircuitBreakerException INSTANCE = new RedisCircuitBreakerException();

    public RedisCircuitBreakerException() {
        super("Circuit breaker is not in closed state, request cannot be processed!");
    }

}
