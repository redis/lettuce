// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

/**
 * Keyword modifiers for redis commands.
 *
 * @author Will Glozer
 */
public enum CommandKeyword {
    AFTER, AGGREGATE, ALPHA, ASC, BEFORE, BY, DESC, LIMIT, MAX, MIN,
    NO, OBJECT, ONE, RESETSTAT, STORE, SUM, WEIGHTS, WITHSCORES;

    public byte[] bytes;

    private CommandKeyword() {
        bytes = name().getBytes();
    }
}
