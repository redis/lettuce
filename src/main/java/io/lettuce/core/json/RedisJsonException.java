/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

public class RedisJsonException extends RuntimeException {

    public RedisJsonException(String message) {
        super(message);
    }

    public RedisJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisJsonException(Throwable cause) {
        super(cause);
    }

}
