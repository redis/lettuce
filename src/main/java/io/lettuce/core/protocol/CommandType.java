/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

    ACL, AUTH, AUTH2,

    // Connection

    ECHO, HELLO, PING, QUIT, READONLY, READWRITE, SELECT, SWAPDB,

    // Server

    BGREWRITEAOF, BGSAVE, CLIENT, COMMAND, CONFIG, DBSIZE, DEBUG, FLUSHALL, FLUSHDB, INFO, MYID, MYSHARDID, LASTSAVE, REPLICAOF, ROLE, MONITOR, SAVE, SHUTDOWN, SLAVEOF, SLOWLOG, SYNC, MEMORY,

    // Keys

    COPY, DEL, DELEX, DUMP, EXISTS, HEXPIRE, EXPIRE, HEXPIREAT, EXPIREAT, HEXPIRETIME, EXPIRETIME, KEYS, MIGRATE, MOVE, OBJECT, HPERSIST, PERSIST, PEXPIRE, HPEXPIRE, PEXPIREAT, HPEXPIREAT, PEXPIRETIME, HPEXPIRETIME, PTTL, HPTTL, RANDOMKEY, RENAME, RENAMENX, RESTORE, TOUCH, TTL, HTTL, TYPE, SCAN, UNLINK,

    // String

    APPEND, GET, GETDEL, GETEX, GETRANGE, GETSET, DIGEST, MGET, MSET, MSETNX, MSETEX, SET, SETEX, PSETEX, SETNX, SETRANGE, STRLEN, STRALGO, LCS,

    // Numeric

    DECR, DECRBY, INCR, INCRBY, INCRBYFLOAT,

    // List

    BLMOVE, BLMPOP, BLPOP, BRPOP, BRPOPLPUSH, LINDEX, LINSERT, LLEN, LMOVE, LMPOP, LPOP, LPOS, LPUSH, LPUSHX, LRANGE, LREM, LSET, LTRIM, RPOP, RPOPLPUSH, RPUSH, RPUSHX, SORT, SORT_RO,

    // Hash

    HDEL, HEXISTS, HGET, HGETDEL, HGETALL, HINCRBY, HINCRBYFLOAT, HKEYS, HLEN, HSTRLEN, HMGET, HMSET, HRANDFIELD, HSET, HSETEX, HGETEX, HSETNX, HVALS, HSCAN,

    // Transaction

    DISCARD, EXEC, MULTI, UNWATCH, WATCH,

    // HyperLogLog

    PFADD, PFCOUNT, PFMERGE,

    // Pub/Sub

    PSUBSCRIBE, PUBLISH, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE, PUBSUB, SSUBSCRIBE, SPUBLISH, SUNSUBSCRIBE,

    // Sets

    SADD, SCARD, SDIFF, SDIFFSTORE, SINTER, SINTERCARD, SINTERSTORE, SISMEMBER, SMISMEMBER, SMEMBERS, SMOVE, SPOP, SRANDMEMBER, SREM, SSCAN, SUNION, SUNIONSTORE,

    // Sorted Set

    BZMPOP, BZPOPMIN, BZPOPMAX, ZADD, ZCARD, ZCOUNT, ZDIFF, ZDIFFSTORE, ZINCRBY, ZINTER, ZINTERCARD, ZINTERSTORE, ZLEXCOUNT, ZMSCORE, ZMPOP, ZPOPMIN, ZPOPMAX, ZRANDMEMBER, ZRANGE, ZRANGEBYSCORE, ZRANGESTORE, ZRANK, ZREM, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREVRANGE, ZREVRANGEBYLEX, ZREVRANGEBYSCORE, ZREVRANK, ZSCAN, ZSCORE, ZUNION, ZUNIONSTORE, ZREMRANGEBYLEX, ZRANGEBYLEX,

    // Functions

    FCALL, FCALL_RO, FUNCTION,

    // Scripting

    EVAL, EVAL_RO, EVALSHA, EVALSHA_RO, SCRIPT,

    // Bits

    BITCOUNT, BITFIELD, BITOP, GETBIT, SETBIT, BITPOS,

    // Geo
    GEOADD, GEODIST, GEOHASH, GEOENCODE, GEODECODE, GEOPOS, GEORADIUS, GEORADIUS_RO, GEORADIUSBYMEMBER, GEORADIUSBYMEMBER_RO, GEOSEARCH, GEOSEARCHSTORE,

    // Stream

    XACK, XACKDEL, XADD, XAUTOCLAIM, XCLAIM, XDEL, XDELEX, XGROUP, XINFO, XLEN, XPENDING, XRANGE, XREVRANGE, XREAD, XREADGROUP, XTRIM,

    // JSON

    JSON_ARRAPPEND("JSON.ARRAPPEND"), JSON_ARRINDEX("JSON.ARRINDEX"), JSON_ARRINSERT("JSON.ARRINSERT"), JSON_ARRLEN(
            "JSON.ARRLEN"), JSON_ARRPOP("JSON.ARRPOP"), JSON_ARRTRIM("JSON.ARRTRIM"), JSON_CLEAR("JSON.CLEAR"), JSON_DEL(
                    "JSON.DEL"), JSON_GET("JSON.GET"), JSON_MERGE("JSON.MERGE"), JSON_MGET("JSON.MGET"), JSON_MSET(
                            "JSON.MSET"), JSON_NUMINCRBY("JSON.NUMINCRBY"), JSON_OBJKEYS("JSON.OBJKEYS"), JSON_OBJLEN(
                                    "JSON.OBJLEN"), JSON_SET("JSON.SET"), JSON_STRAPPEND("JSON.STRAPPEND"), JSON_STRLEN(
                                            "JSON.STRLEN"), JSON_TOGGLE("JSON.TOGGLE"), JSON_TYPE("JSON.TYPE"),

    // Vector Set

    VADD, VCARD, VDIM, VEMB, VEMBRAW, VGETATTR, VINFO, VLINKS, VLINKSWITHSCORES, VRANDMEMBER, VREM, VSETATTR, VSIM, VSIMWITHSCORES,

    // RediSearch
    FT_AGGREGATE("FT.AGGREGATE"), FT_ALIASADD("FT.ALIASADD"), FT_ALIASDEL("FT.ALIASDEL"), FT_ALIASUPDATE(
            "FT.ALIASUPDATE"), FT_ALTER("FT.ALTER"), FT_CREATE("FT.CREATE"), FT_CURSOR("FT.CURSOR"), FT_DICTADD(
                    "FT.DICTADD"), FT_DICTDEL("FT.DICTDEL"), FT_DICTDUMP("FT.DICTDUMP"), FT_DROPINDEX(
                            "FT.DROPINDEX"), FT_EXPLAIN("FT.EXPLAIN"), FT_LIST("FT._LIST"), FT_SEARCH(
                                    "FT.SEARCH"), FT_SPELLCHECK("FT.SPELLCHECK"), FT_SUGADD("FT.SUGADD"), FT_SUGDEL(
                                            "FT.SUGDEL"), FT_SUGGET("FT.SUGGET"), FT_SUGLEN("FT.SUGLEN"), FT_SYNDUMP(
                                                    "FT.SYNDUMP"), FT_SYNUPDATE("FT.SYNUPDATE"), FT_TAGVALS("FT.TAGVALS"),

    // Others

    TIME, WAIT,

    // SENTINEL

    SENTINEL,

    // CLUSTER

    ASKING, CLUSTER;

    public final byte[] bytes;

    private final String command;

    /**
     * Simple commands (comprised of only letters) use the name of the enum constant as command name.
     */
    CommandType() {
        command = name();
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Complex commands (comprised of other symbols besides letters) get the command name as a parameter.
     *
     * @param name the command name, must not be {@literal null}.
     */
    CommandType(String name) {
        command = name;
        bytes = name.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     *
     * @return name of the command.
     */
    public String toString() {
        return command;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
