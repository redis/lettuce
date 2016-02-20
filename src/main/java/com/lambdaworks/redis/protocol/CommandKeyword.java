// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

/**
 * Keyword modifiers for redis commands.
 * 
 * @author Will Glozer
 */
public enum CommandKeyword implements ProtocolKeyword {
    ADDR, ADDSLOTS, AFTER, AGGREGATE, ALPHA, AND, ASK, ASC, ASYNC, BEFORE, BUMPEPOCH, BY, CHANNELS, COPY, COUNT, COUNTKEYSINSLOT, DELSLOTS, DESC, SOFT, HARD, ENCODING,

    FAILOVER, FORGET, FLUSH, FORCE, FLUSHSLOTS, GETNAME, GETKEYSINSLOT, HTSTATS, ID, IDLETIME, KILL, KEYSLOT, LEN, LIMIT, LIST, LOAD, MATCH,

    MAX, MEET, MIN, MOVED, NO, NODE, NODES, NOSAVE, NOT, NUMSUB, NUMPAT, ONE, OR, PAUSE, REFCOUNT, REMOVE, RELOAD, REPLACE, REPLICATE, RESET,

    RESETSTAT, RESTART, REWRITE, SAVECONFIG, SDSLEN, SETNAME, SETSLOT, SLOTS, STABLE, MIGRATING, IMPORTING, SKIPME, SLAVES, STORE, SUM, SEGFAULT, WEIGHTS,

    WITHSCORES, XOR;

    public final byte[] bytes;

    private CommandKeyword() {
        bytes = name().getBytes(LettuceCharsets.ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }
}
