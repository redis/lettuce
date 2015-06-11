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

    public RedisCommandTimeoutException(Throwable cause) {
        super(cause);
    }

    public RedisCommandTimeoutException(String msg) {
        super(msg);
    }

    public RedisCommandTimeoutException(String msg, Throwable e) {
        super(msg, e);
    }
}
