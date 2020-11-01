/*
 * Copyright 2011-2020 the original author or authors.
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
 * Redis commands.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Zhang Jessey
 * @author dengliming
 * @author Mikhael Sokolov
 */
public enum CommandType implements ProtocolKeyword {

    // Authentication

    ACL, AUTH,

    // Connection

    ECHO, HELLO, PING, QUIT, READONLY, READWRITE, SELECT, SWAPDB,

    // Server

    BGREWRITEAOF, BGSAVE, CLIENT, COMMAND, CONFIG, DBSIZE, DEBUG, FLUSHALL, FLUSHDB, INFO, MYID, LASTSAVE, ROLE, MONITOR, SAVE, SHUTDOWN, SLAVEOF, SLOWLOG, SYNC, MEMORY,

    // Keys

    DEL, DUMP, EXISTS, EXPIRE, EXPIREAT, KEYS, MIGRATE, MOVE, OBJECT, PERSIST, PEXPIRE, PEXPIREAT, PTTL, RANDOMKEY, RENAME, RENAMENX, RESTORE, TOUCH, TTL, TYPE, SCAN, UNLINK,

    // String

    APPEND, GET, GETRANGE, GETSET, MGET, MSET, MSETNX, SET, SETEX, PSETEX, SETNX, SETRANGE, STRLEN, STRALGO,

    // Numeric

    DECR, DECRBY, INCR, INCRBY, INCRBYFLOAT,

    // List

    BLMOVE, BLPOP, BRPOP, BRPOPLPUSH, LINDEX, LINSERT, LLEN, LMOVE, LPOP, LPOS, LPUSH, LPUSHX, LRANGE, LREM, LSET, LTRIM, RPOP, RPOPLPUSH, RPUSH, RPUSHX, SORT,

    // Hash

    HDEL, HEXISTS, HGET, HGETALL, HINCRBY, HINCRBYFLOAT, HKEYS, HLEN, HSTRLEN, HMGET, HMSET, HSET, HSETNX, HVALS, HSCAN,

    // Transaction

    DISCARD, EXEC, MULTI, UNWATCH, WATCH,

    // HyperLogLog

    PFADD, PFCOUNT, PFMERGE,

    // Pub/Sub

    PSUBSCRIBE, PUBLISH, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE, PUBSUB,

    // Sets

    SADD, SCARD, SDIFF, SDIFFSTORE, SINTER, SINTERSTORE, SISMEMBER, SMISMEMBER, SMEMBERS, SMOVE, SPOP, SRANDMEMBER, SREM, SSCAN, SUNION, SUNIONSTORE,

    // Sorted Set

    BZPOPMIN, BZPOPMAX, ZADD, ZCARD, ZCOUNT, ZINCRBY, ZINTER, ZINTERSTORE, ZLEXCOUNT, ZMSCORE, ZPOPMIN, ZPOPMAX, ZRANGE, ZRANGEBYSCORE, ZRANK, ZREM, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREVRANGE, ZREVRANGEBYLEX, ZREVRANGEBYSCORE, ZREVRANK, ZSCAN, ZSCORE, ZUNION, ZUNIONSTORE, ZREMRANGEBYLEX, ZRANGEBYLEX,

    // Scripting

    EVAL, EVALSHA, SCRIPT,

    // Bits

    BITCOUNT, BITFIELD, BITOP, GETBIT, SETBIT, BITPOS,

    // Geo

    GEOADD, GEORADIUS, GEORADIUS_RO, GEORADIUSBYMEMBER, GEORADIUSBYMEMBER_RO, GEOENCODE, GEODECODE, GEOPOS, GEODIST, GEOHASH,

    // Stream

    XACK, XADD, XCLAIM, XDEL, XGROUP, XINFO, XLEN, XPENDING, XRANGE, XREVRANGE, XREAD, XREADGROUP, XTRIM,

    // Others

    TIME, WAIT,

    // SENTINEL

    SENTINEL,

    // CLUSTER

    ASKING, CLUSTER;

    public final byte[] bytes;

    CommandType() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
