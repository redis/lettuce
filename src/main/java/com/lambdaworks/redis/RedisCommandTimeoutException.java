package com.lambdaworks.redis;

/**
 * Exception thrown when the command waiting timeout is exceeded.
 * 
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisCommandTimeoutException extends RedisException {

    public RedisCommandTimeoutException() {
        super("Command timed out");
    }

    public RedisCommandTimeoutException(String msg) {
        super(msg);
    }
}
