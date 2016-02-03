package com.lambdaworks.redis;

/**
 * Exception for connection failures.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@SuppressWarnings("serial")
public class RedisConnectionException extends RedisException {

    public RedisConnectionException(String msg) {
        super(msg);
    }

    public RedisConnectionException(String msg, Throwable e) {
        super(msg, e);
    }

}
