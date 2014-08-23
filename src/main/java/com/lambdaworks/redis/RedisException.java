// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

/**
 * Exception thrown when redis returns an error message, or when the client fails for any reason.
 * 
 * @author Will Glozer
 */
@SuppressWarnings("serial")
public class RedisException extends RuntimeException {
    /**
     * 
     * @param msg
     */
    public RedisException(String msg) {
        super(msg);
    }

    /**
     * 
     * @param msg
     * @param e
     */
    public RedisException(String msg, Throwable e) {
        super(msg, e);
    }

    /**
     * 
     * @param cause
     */
    public RedisException(Throwable cause) {
        super(cause);
    }
}
