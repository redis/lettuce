package com.lambdaworks.redis;

/**
 * Exception for errors states reported by Redis.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisCommandExecutionException extends RedisException {

    public RedisCommandExecutionException(Throwable cause) {
        super(cause);
    }

    public RedisCommandExecutionException(String msg) {
        super(msg);
    }

    public RedisCommandExecutionException(String msg, Throwable e) {
        super(msg, e);
    }

}
