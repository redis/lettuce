// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.*;
import com.lambdaworks.redis.protocol.*;
import org.jboss.netty.channel.*;

import java.util.*;
import java.util.concurrent.*;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;
import static com.lambdaworks.redis.protocol.CommandType.*;

/**
 * A synchronous thread-safe connection to a redis server. Multiple threads may
 * share one {@link RedisConnection} provided they avoid blocking and transactional
 * operations such as {@link #blpop} and {@link #multi()}/{@link #exec}.
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects
 * automatically until {@link #close} is called. All pending commands will be
 * (re)sent after successful reconnection.
 *
 * @author Will Glozer
 */
public class RedisConnection<K, V> extends SimpleChannelUpstreamHandler {
    protected BlockingQueue<Command<?>> queue;
    protected RedisCodec<K, V> codec;
    protected Channel channel;
    private int timeout;
    private TimeUnit unit;
    private String password;
    private int db;
    private MultiOutput multi;
    private boolean closed;

    /**
     * Initialize a new connection.
     *
     * @param queue   Command queue.
     * @param codec   Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a responses.
     * @param unit    Unit of time for the timeout.
     */
    public RedisConnection(BlockingQueue<Command<?>> queue, RedisCodec<K, V> codec, int timeout, TimeUnit unit) {
        this.queue = queue;
        this.codec = codec;
        this.timeout = timeout;
        this.unit = unit;
    }

    /**
     * Set the command timeout for this connection.
     *
     * @param timeout Command timeout.
     * @param unit    Unit of time for the timeout.
     */
    public void setTimeout(int timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    public Long append(K key, V value) {
        Command<Long> cmd = dispatch(APPEND, new IntegerOutput(codec), key, value);
        return getOutput(cmd);
    }

    public String auth(String password) {
        this.password = password;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(password);
        Command<String> cmd = dispatch(AUTH, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String bgrewriteaof() {
        Command<String> cmd = dispatch(BGREWRITEAOF, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public String bgsave() {
        Command<String> cmd = dispatch(BGSAVE, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public List<V> blpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        Command<List<V>> cmd = dispatch(BLPOP, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> brpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        Command<List<V>> cmd = dispatch(BRPOP, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public V brpoplpush(long timeout, K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(source).addKey(destination).add(timeout);
        Command<V> cmd = dispatch(BRPOPLPUSH, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<String> configGet(String parameter) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GET).add(parameter);
        Command<List<String>> cmd = dispatch(CONFIG, new StringListOutput(codec), args);
        return getOutput(cmd);
    }

    public String configResetstat() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(RESETSTAT);
        Command<String> cmd = dispatch(CONFIG, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String configSet(String parameter, String value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SET).add(parameter).add(value);
        Command<String> cmd = dispatch(CONFIG, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Long dbsize() {
        Command<Long> cmd = dispatch(DBSIZE, new IntegerOutput(codec));
        return getOutput(cmd);
    }

    public String debugObject(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(OBJECT).addKey(key);
        Command<String> cmd = dispatch(DEBUG, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Long decr(K key) {
        Command<Long> cmd = dispatch(DECR, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public Long decrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        Command<Long> cmd = dispatch(DECRBY, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Long del(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<Long> cmd = dispatch(DEL, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String discard() {
        Command<String> cmd = dispatch(DISCARD, new StatusOutput(codec));
        multi = null;
        return getOutput(cmd);
    }

    public V echo(V msg) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addValue(msg);
        Command<V> cmd = dispatch(ECHO, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean exists(K key) {
        Command<Boolean> cmd = dispatch(EXISTS, new BooleanOutput(codec), key);
        return getOutput(cmd);
    }

    public Boolean expire(K key, long seconds) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds);
        Command<Boolean> cmd = dispatch(EXPIRE, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean expireat(K key, Date timestamp) {
        long seconds = timestamp.getTime() / 1000;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds);
        Command<Boolean> cmd = dispatch(EXPIREAT, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public List<Object> exec() {
        Command<List<Object>> cmd = dispatch(EXEC, multi);
        multi = null;
        return getOutput(cmd);
    }

    public String flushall() throws Exception {
        Command<String> cmd = dispatch(FLUSHALL, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public String flushdb() throws Exception {
        Command<String> cmd = dispatch(FLUSHDB, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public V get(K key) {
        Command<V> cmd = dispatch(GET, new ValueOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public Long getbit(K key, long offset) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset);
        Command<Long> cmd = dispatch(GETBIT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public V getrange(K key, long start, long end) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(end);
        Command<V> cmd = dispatch(GETRANGE, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public V getset(K key, V value) {
        Command<V> cmd = dispatch(GETSET, new ValueOutput<V>(codec), key, value);
        return getOutput(cmd);
    }

    public Boolean hdel(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        Command<Boolean> cmd = dispatch(HDEL, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean hexists(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        Command<Boolean> cmd = dispatch(HEXISTS, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public V hget(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        Command<V> cmd = dispatch(HGET, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long hincrby(K key, K field, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).add(amount);
        Command<Long> cmd = dispatch(HINCRBY, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Map<K, V> hgetall(K key) {
        Command<Map<K, V>> cmd = dispatch(HGETALL, new MapOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public List<K> hkeys(K key) {
        Command<List<K>> cmd = dispatch(HKEYS, new KeyListOutput<K>(codec), key);
        return getOutput(cmd);
    }

    public Long hlen(K key) {
        Command<Long> cmd = dispatch(HLEN, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public List<V> hmget(K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        Command<List<V>> cmd = dispatch(HMGET, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public String hmset(K key, Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(map);
        Command<String> cmd = dispatch(HMSET, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean hset(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        Command<Boolean> cmd = dispatch(HSET, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean hsetnx(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        Command<Boolean> cmd = dispatch(HSETNX, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public List<V> hvals(K key) {
        Command<List<V>> cmd = dispatch(HVALS, new ValueListOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public Long incr(K key) {
        Command<Long> cmd = dispatch(INCR, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public Long incrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        Command<Long> cmd = dispatch(INCRBY, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String info() {
        Command<String> cmd = dispatch(INFO, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public List<K> keys(K pattern) {
        Command<List<K>> cmd = dispatch(KEYS, new KeyListOutput<K>(codec), pattern);
        return getOutput(cmd);
    }

    public Date lastsave() {
        Command<Date> cmd = dispatch(LASTSAVE, new DateOutput(codec));
        return getOutput(cmd);
    }

    public V lindex(K key, long index) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index);
        Command<V> cmd = dispatch(LINDEX, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long linsert(K key, boolean before, V pivot, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(before ? BEFORE : AFTER).addValue(pivot).addValue(value);
        Command<Long> cmd = dispatch(LINSERT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Long llen(K key) {
        Command<Long> cmd = dispatch(LLEN, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public V lpop(K key) {
        Command<V> cmd = dispatch(LPOP, new ValueOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public Long lpush(K key, V value) {
        Command<Long> cmd = dispatch(LPUSH, new IntegerOutput(codec), key, value);
        return getOutput(cmd);
    }

    public Long lpushx(K key, V value) {
        Command<Long> cmd = dispatch(LPUSHX, new IntegerOutput(codec), key, value);
        return getOutput(cmd);
    }

    public List<V> lrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<List<V>> cmd = dispatch(LRANGE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long lrem(K key, long count, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(count).addValue(value);
        Command<Long> cmd = dispatch(LREM, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String lset(K key, long index, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index).addValue(value);
        Command<String> cmd = dispatch(LSET, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String ltrim(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<String> cmd = dispatch(LTRIM, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public List<V> mget(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<List<V>> cmd = dispatch(MGET, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean move(K key, int db) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(db);
        Command<Boolean> cmd = dispatch(MOVE, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public String multi() {
        Command<String> cmd = dispatch(MULTI, new StatusOutput(codec));
        String status = getOutput(cmd);
        if ("OK".equals(status)) {
            multi = new MultiOutput(codec);
        }
        return status;
    }

    public String mset(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        Command<String> cmd = dispatch(MSET, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean msetnx(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        Command<Boolean> cmd = dispatch(MSETNX, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public String objectEncoding(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(ENCODING).addKey(key);
        Command<String> cmd = dispatch(OBJECT, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Long objectIdletime(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(IDLETIME).addKey(key);
        Command<Long> cmd = dispatch(OBJECT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Long objectRefcount(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(REFCOUNT).addKey(key);
        Command<Long> cmd = dispatch(OBJECT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean persist(K key) {
        Command<Boolean> cmd = dispatch(PERSIST, new BooleanOutput(codec), key);
        return getOutput(cmd);
    }

    public String ping() {
        Command<String> cmd = dispatch(PING, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public Long publish(String channel, V message) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(channel).addValue(message);
        Command<Long> cmd = dispatch(PUBLISH, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String quit() {
        Command<String> cmd = dispatch(QUIT, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public V randomkey() {
        Command<V> cmd = dispatch(RANDOMKEY, new ValueOutput<V>(codec));
        return getOutput(cmd);
    }

    public String rename(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        Command<String> cmd = dispatch(RENAME, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean renamenx(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        Command<Boolean> cmd = dispatch(RENAMENX, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public V rpop(K key) {
        Command<V> cmd = dispatch(RPOP, new ValueOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public V rpoplpush(K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination);
        Command<V> cmd = dispatch(RPOPLPUSH, new ValueOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long rpush(K key, V value) {
        Command<Long> cmd = dispatch(RPUSH, new IntegerOutput(codec), key, value);
        return getOutput(cmd);
    }

    public Long rpushx(K key, V value) {
        Command<Long> cmd = dispatch(RPUSHX, new IntegerOutput(codec), key, value);
        return getOutput(cmd);
    }

    public Boolean sadd(K key, V member) {
        Command<Boolean> cmd = dispatch(SADD, new BooleanOutput(codec), key, member);
        return getOutput(cmd);
    }

    public String save() {
        Command<String> cmd = dispatch(SAVE, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public Long scard(K key) {
        Command<Long> cmd = dispatch(SCARD, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public Set<V> sdiff(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<Set<V>> cmd = dispatch(SDIFF, new ValueSetOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long sdiffstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<Long> cmd = dispatch(SDIFFSTORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String select(int db) {
        this.db = db;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(db);
        Command<String> cmd = dispatch(SELECT, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String set(K key, V value) {
        Command<String> cmd = dispatch(SET, new StatusOutput(codec), key, value);
        return getOutput(cmd);
    }

    public Long setbit(K key, long offset, int value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).add(value);
        Command<Long> cmd = dispatch(SETBIT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String setex(K key, long seconds, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds).addValue(value);
        Command<String> cmd = dispatch(SETEX, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean setnx(K key, V value) {
        Command<Boolean> cmd = dispatch(SETNX, new BooleanOutput(codec), key, value);
        return getOutput(cmd);
    }

    public Long setrange(K key, long offset, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).addValue(value);
        Command<Long> cmd = dispatch(SETRANGE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public void shutdown() {
        dispatch(SHUTDOWN, new StatusOutput(codec));
    }

    public Set<V> sinter(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<Set<V>> cmd = dispatch(SINTER, new ValueSetOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long sinterstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<Long> cmd = dispatch(SINTERSTORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Boolean sismember(K key, V member) {
        Command<Boolean> cmd = dispatch(SISMEMBER, new BooleanOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Boolean smove(K source, K destination, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination).addValue(member);
        Command<Boolean> cmd = dispatch(SMOVE, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public String slaveof(String host, int port) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(host).add(port);
        Command<String> cmd = dispatch(SLAVEOF, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String slaveofNoOne() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NO).add(ONE);
        Command<String> cmd = dispatch(SLAVEOF, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public Set<V> smembers(K key) {
        Command<Set<V>> cmd = dispatch(SMEMBERS, new ValueSetOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public List<V> sort(K key) {
        Command<List<V>> cmd = dispatch(SORT, new ValueListOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public List<V> sort(K key, SortArgs sortArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, null);
        Command<List<V>> cmd = dispatch(SORT, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long sortStore(K key, SortArgs sortArgs, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, destination);
        Command<Long> cmd = dispatch(SORT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public V spop(K key) {
        Command<V> cmd = dispatch(SPOP, new ValueOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public V srandmember(K key) {
        Command<V> cmd = dispatch(SRANDMEMBER, new ValueOutput<V>(codec), key);
        return getOutput(cmd);
    }

    public Boolean srem(K key, V member) {
        Command<Boolean> cmd = dispatch(SREM, new BooleanOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Set<V> sunion(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<Set<V>> cmd = dispatch(SUNION, new ValueSetOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long sunionstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<Long> cmd = dispatch(SUNIONSTORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public String sync() {
        Command<String> cmd = dispatch(SYNC, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public Long strlen(K key) {
        Command<Long> cmd = dispatch(STRLEN, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public Long ttl(K key) {
        Command<Long> cmd = dispatch(TTL, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public String type(K key) {
        Command<String> cmd = dispatch(TYPE, new StatusOutput(codec), key);
        return getOutput(cmd);
    }

    public String watch(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<String> cmd = dispatch(WATCH, new StatusOutput(codec), args);
        return getOutput(cmd);
    }

    public String unwatch() {
        Command<String> cmd = dispatch(UNWATCH, new StatusOutput(codec));
        return getOutput(cmd);
    }

    public Boolean zadd(K key, double score, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(score).addValue(member);
        Command<Boolean> cmd = dispatch(ZADD, new BooleanOutput(codec), args);
        return getOutput(cmd);
    }

    public Long zcard(K key) {
        Command<Long> cmd = dispatch(ZCARD, new IntegerOutput(codec), key);
        return getOutput(cmd);
    }

    public Long zcount(K key, double min, double max) {
        return zcount(key, string(min), string(max));
    }

    public Long zcount(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<Long> cmd = dispatch(ZCOUNT, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Double zincrby(K key, double amount, K member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount).addKey(member);
        Command<Double> cmd = dispatch(ZINCRBY, new DoubleOutput(codec), args);
        return getOutput(cmd);
    }

    public Long zinterstore(K destination, K... keys) {
        return zinterstore(destination, new ZStoreArgs(), keys);
    }

    public Long zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        Command<Long> cmd = dispatch(ZINTERSTORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<List<V>> cmd = dispatch(ZRANGE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZRANGE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrangebyscore(K key, double min, double max) {
        return zrangebyscore(key, string(min), string(max));
    }

    public List<V> zrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<List<V>> cmd = dispatch(ZRANGEBYSCORE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrangebyscore(K key, double min, double max, long offset, long count) {
        return zrangebyscore(key, string(min), string(max), offset, count);
    }

    public List<V> zrangebyscore(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        Command<List<V>> cmd = dispatch(ZRANGEBYSCORE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max) {
        return zrangebyscoreWithScores(key, string(min), string(max));
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZRANGEBYSCORE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return zrangebyscoreWithScores(key, string(min), string(max), offset, count);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZRANGEBYSCORE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long zrank(K key, V member) {
        Command<Long> cmd = dispatch(ZRANK, new IntegerOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Boolean zrem(K key, V member) {
        Command<Boolean> cmd = dispatch(ZREM, new BooleanOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Long zremrangebyrank(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<Long> cmd = dispatch(ZREMRANGEBYRANK, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public Long zremrangebyscore(K key, double min, double max) {
        return zremrangebyscore(key, string(min), string(max));
    }

    public Long zremrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<Long> cmd = dispatch(ZREMRANGEBYSCORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<List<V>> cmd = dispatch(ZREVRANGE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZREVRANGE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrangebyscore(K key, double max, double min) {
        return zrevrangebyscore(key, string(max), string(min));
    }

    public List<V> zrevrangebyscore(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(max).add(min);
        Command<List<V>> cmd = dispatch(ZREVRANGEBYSCORE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return zrevrangebyscore(key, string(max), string(min), offset, count);
    }

    public List<V> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(LIMIT).add(offset).add(count);
        Command<List<V>> cmd = dispatch(ZREVRANGEBYSCORE, new ValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return zrevrangebyscoreWithScores(key, string(max), string(min));
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZREVRANGEBYSCORE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return zrevrangebyscoreWithScores(key, string(max), string(min), offset, count);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        Command<List<ScoredValue<V>>> cmd = dispatch(ZREVRANGEBYSCORE, new ScoredValueListOutput<V>(codec), args);
        return getOutput(cmd);
    }

    public Long zrevrank(K key, V member) {
        Command<Long> cmd = dispatch(ZREVRANK, new IntegerOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Double zscore(K key, V member) {
        Command<Double> cmd = dispatch(ZSCORE, new DoubleOutput(codec), key, member);
        return getOutput(cmd);
    }

    public Long zunionstore(K destination, K... keys) {
        return zunionstore(destination, new ZStoreArgs(), keys);
    }

    public Long zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        Command<Long> cmd = dispatch(ZUNIONSTORE, new IntegerOutput(codec), args);
        return getOutput(cmd);
    }

    /**
     * Get a new asynchronous wrapper for this connection. The wrapper delegates
     * all commands to this connection but returns null instead of waiting for
     * a response from the server.
     *
     * @return A new asynchronous connection wrapper.
     */
    public RedisAsyncConnection<K, V> getAsyncConnection() {
        return new RedisAsyncConnection<K, V>(codec, this);
    }

    /**
     * Close the connection.
     */
    public synchronized void close() {
        if (!closed && channel != null) {
            ConnectionWatchdog watchdog = channel.getPipeline().get(ConnectionWatchdog.class);
            watchdog.setReconnect(false);
            closed = true;
            channel.close();
        }
    }

    @Override
    public synchronized void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = ctx.getChannel();

        BlockingQueue<Command<?>> tmp = new LinkedBlockingQueue<Command<?>>();

        if (password != null) {
            CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(password);
            tmp.put(new Command<String>(AUTH, new StatusOutput(codec), args));
        }

        if (db != 0) {
            CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(db);
            tmp.put(new Command<String>(SELECT, new StatusOutput(codec), args));
        }

        tmp.addAll(queue);

        queue.clear();
        queue.addAll(tmp);

        for (Command cmd : queue) {
            channel.write(cmd);
        }
    }

    @Override
    public synchronized void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (closed) {
            for (Command<?> cmd : queue) {
                cmd.getOutput().setError("Connection closed");
                cmd.complete();
            }
            queue.clear();
            queue = null;
            channel = null;
        }
    }

    public <T> Command<T> dispatch(CommandType type, CommandOutput<T> output) {
        return dispatch(type, output, null);
    }

    public <T> Command<T> dispatch(CommandType type, CommandOutput<T> output, K key, V... values) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValues(values);
        return dispatch(type, output, args);
    }

    public synchronized <T> Command<T> dispatch(CommandType type, CommandOutput<T> output, CommandArgs<K, V> args) {
        Command<T> cmd = new Command<T>(type, output, args);

        try {
            if (multi != null && type != EXEC) {
                multi.add(cmd.getOutput());
            }

            queue.put(cmd);

            if (channel != null) {
                channel.write(cmd);
            }
        } catch (NullPointerException e) {
            throw new RedisException("Connection is closed");
        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }

        return cmd;
    }

    public <T> T getOutput(Command<T> cmd) {
        if (!cmd.await(timeout, unit)) {
            throw new RedisException("Command timed out");
        }
        return cmd.get();
    }

    public String string(double n) {
        if (Double.isInfinite(n)) {
            return (n > 0) ? "+inf" : "-inf";
        }
        return Double.toString(n);
    }
}
