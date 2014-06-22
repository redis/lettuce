// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

/**
 * Keyword modifiers for redis commands.
 * 
 * @author Will Glozer
 */
public enum CommandKeyword {
    ADDSLOTS, AFTER, AGGREGATE, ALPHA, AND, ASC, BEFORE, BY, CHANNELS, COUNT, DELSLOTS, DESC, SOFT, HARD, ENCODING, FAILOVER, FORGET, FLUSH, FORCE, FLUSHSLOTS, GETNAME, GETKEYSINSLOT, IDLETIME, KILL, LEN, LIMIT, LIST, LOAD, MATCH, MAX, MEET, MIN, MOVED, NO, NODE, NODES, NOSAVE, NOT, NUMSUB, NUMPAT, ONE, OR, PAUSE, REFCOUNT, REPLICATE, RESET, REWRITE, RESETSTAT, SETNAME, SETSLOT, MIGRATING, IMPORTING, SLAVES, STORE, SUM, SEGFAULT, WEIGHTS, WITHSCORES, XOR, REMOVE;

    public final byte[] bytes;

    private CommandKeyword() {
        bytes = name().getBytes(LettuceCharsets.ASCII);
    }
}
