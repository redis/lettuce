package com.lambdaworks.redis.cluster;

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

    public final static ProtocolKeyword READ_ONLY_COMMANDS[];

    static {

        Set<ProtocolKeyword> set = new HashSet<ProtocolKeyword>(CommandName.values().length);

        for (CommandName commandNames : CommandName.values()) {
            set.add(CommandType.valueOf(commandNames.name()));
        }

        READ_ONLY_COMMANDS = set.toArray(new ProtocolKeyword[set.size()]);
    }

    enum CommandName {
        ASKING, BITCOUNT, BITPOS, CLIENT, COMMAND, DUMP, ECHO, EXISTS, //
        GEODIST, GEOPOS, GEORADIUS, GEORADIUSBYMEMBER, GEOHASH, GET, GETBIT, //
        GETRANGE, HEXISTS, HGET, HGETALL, HKEYS, HLEN, HMGET, HSCAN, HSTRLEN, //
        HVALS, INFO, KEYS, LINDEX, LLEN, LRANGE, MGET, MULTI, PFCOUNT, PTTL, //
        RANDOMKEY, READWRITE, SCAN, SCARD, SCRIPT, //
        SDIFF, SINTER, SISMEMBER, SMEMBERS, SRANDMEMBER, SSCAN, STRLEN, //
        SUNION, TIME, TTL, TYPE, WAIT, ZCARD, ZCOUNT, ZLEXCOUNT, ZRANGE, //
        ZRANGEBYLEX, ZRANGEBYSCORE, ZRANK, ZREVRANGE, /* ZREVRANGEBYLEX , */ZREVRANGEBYSCORE, ZREVRANK, ZSCAN, ZSCORE, //

        // Pub/Sub commands are no key-space commands so they are safe to execute on slave nodes
        PUBLISH, PUBSUB, PSUBSCRIBE, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE

    }

}
