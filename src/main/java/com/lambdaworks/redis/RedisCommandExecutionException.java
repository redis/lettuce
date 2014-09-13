package com.lambdaworks.redis;

/**
 * Exception for errors states reported by Redis.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisCommandExecutionException extends RedisException {

    public RedisCommandExecutionException(String msg) {
        super(msg);
    }
}
