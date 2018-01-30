/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

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
