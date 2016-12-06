/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.masterslave;

import java.util.HashSet;
import java.util.Set;

import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * Contains all command names that are read-only commands.
 *
 * @author Mark Paluch
 */
class ReadOnlyCommands {

    public static final ProtocolKeyword READ_ONLY_COMMANDS[];

    static {

        Set<ProtocolKeyword> set = new HashSet<ProtocolKeyword>(CommandName.values().length);

        for (CommandName commandNames : CommandName.values()) {
            set.add(CommandType.valueOf(commandNames.name()));
        }

        READ_ONLY_COMMANDS = set.toArray(new ProtocolKeyword[set.size()]);
    }

    enum CommandName {
        ASKING, BITCOUNT, BITPOS, CLIENT, COMMAND, DUMP, ECHO, EVAL, EVALSHA, EXISTS, //
        GEODIST, GEOPOS, GEORADIUS, GEORADIUSBYMEMBER, GEOHASH, GET, GETBIT, //
        GETRANGE, HEXISTS, HGET, HGETALL, HKEYS, HLEN, HMGET, HSCAN, HSTRLEN, //
        HVALS, INFO, KEYS, LINDEX, LLEN, LRANGE, MGET, MULTI, PFCOUNT, PTTL, //
        RANDOMKEY, READWRITE, SCAN, SCARD, SCRIPT, //
        SDIFF, SINTER, SISMEMBER, SMEMBERS, SRANDMEMBER, SSCAN, STRLEN, //
        SUNION, TIME, TTL, TYPE, ZCARD, ZCOUNT, ZLEXCOUNT, ZRANGE, //
        ZRANGEBYLEX, ZRANGEBYSCORE, ZRANK, ZREVRANGE, ZREVRANGEBYLEX, ZREVRANGEBYSCORE, ZREVRANK, ZSCAN, ZSCORE,
    }
}
