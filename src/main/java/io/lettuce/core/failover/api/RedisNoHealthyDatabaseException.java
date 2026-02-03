package io.lettuce.core.failover.api;

import io.lettuce.core.RedisConnectionException;

public class RedisNoHealthyDatabaseException extends RedisConnectionException {

    public RedisNoHealthyDatabaseException(String message) {
        super(message);
    }

}
