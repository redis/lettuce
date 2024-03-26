package io.lettuce.core.protocol;

import io.lettuce.core.RedisException;

/**
 * Exception thrown on Redis protocol failures.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class RedisProtocolException extends RedisException {

    public RedisProtocolException(String msg) {
        super(msg);
    }

}
