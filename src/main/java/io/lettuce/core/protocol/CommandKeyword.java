/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Keyword modifiers for redis commands.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Zhang Jessey
 * @author dengliming
 */
public enum CommandKeyword implements ProtocolKeyword {

    ABSTTL, ADDR, ADDSLOTS, ADDSLOTSRANGE, AFTER, AGGREGATE, ALLCHANNELS, ALLCOMMANDS, ALLKEYS, ALPHA, AND, ASK, ASC, ASYNC, BEFORE, BLOCK, BUMPEPOCH,

    BY, BYLEX, BYSCORE, CACHING, CAT, CH, CHANNELS, COPY, COUNT, COUNTKEYSINSLOT, CONSUMERS, CREATE, DB, DELSLOTS, DELSLOTSRANGE, DELUSER, DESC, DRYRUN, SOFT, HARD, ENCODING,

    FAILOVER, FORGET, FLUSH, FORCE, FREQ, FLUSHSLOTS, GENPASS, GETNAME, GETUSER, GETKEYSINSLOT, GETREDIR, GROUP, GROUPS, HTSTATS, ID, IDLE,

    IDLETIME, JUSTID, KILL, KEYSLOT, LEFT, LEN, LIMIT, LIST, LOAD, LOG, MATCH,

    MAX, MAXLEN, MEET, MIN, MINID, MOVED, NO, NOACK, NOCOMMANDS, NODE, NODES, NOMKSTREAM, NOPASS, NOSAVE, NOT, NUMSUB, NUMPAT, NX, OFF, ON, ONE, OR, PAUSE,

    REFCOUNT, REMOVE, RELOAD, REPLACE, REPLICATE, REPLICAS, REV, RESET, RESETCHANNELS, RESETKEYS, RESETPASS,

    RESETSTAT, RESTART, RETRYCOUNT, REWRITE, RIGHT, SAVECONFIG, SDSLEN, SETNAME, SETSLOT, SHARDS, SLOTS, STABLE,

    MIGRATING, IMPORTING, SAVE, SKIPME, SLAVES, STREAM, STORE, SUM, SEGFAULT, SETUSER, TRACKING, TYPE, UNBLOCK, USERS, USAGE, WEIGHTS, WHOAMI,

    WITHSCORES, WITHVALUES, XOR, XX, YES;

    public final byte[] bytes;

    private CommandKeyword() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
