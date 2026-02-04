package io.lettuce.core.failover.api;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.annotations.Experimental;

@Experimental
public class RedisNoHealthyDatabaseException extends RedisConnectionException {

    public RedisNoHealthyDatabaseException(String message) {
        super(message);
    }

}
