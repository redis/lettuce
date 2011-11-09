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
    protected BlockingQueue<Command<K, V, ?>> queue;
    protected RedisCodec<K, V> codec;
    protected Channel channel;
    private int timeout;
    private TimeUnit unit;
    private String password;
    private int db;
    private MultiOutput<K, V> multi;
    private boolean closed;

    /**
     * Initialize a new connection.
     *
     * @param queue   Command queue.
     * @param codec   Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a responses.
     * @param unit    Unit of time for the timeout.
     */
    public RedisConnection(BlockingQueue<Command<K, V, ?>> queue, RedisCodec<K, V> codec, int timeout, TimeUnit unit) {
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
        Command<K, V, Long> cmd = dispatch(APPEND, new IntegerOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public String auth(String password) {
        this.password = password;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(password);
        Command<K, V, String> cmd = dispatch(AUTH, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String bgrewriteaof() {
        Command<K, V, String> cmd = dispatch(BGREWRITEAOF, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public String bgsave() {
        Command<K, V, String> cmd = dispatch(BGSAVE, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public List<V> blpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        Command<K, V, List<V>> cmd = dispatch(BLPOP, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> brpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        Command<K, V, List<V>> cmd = dispatch(BRPOP, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V brpoplpush(long timeout, K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(source).addKey(destination).add(timeout);
        Command<K, V, V> cmd = dispatch(BRPOPLPUSH, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String clientKill(String addr) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KILL).add(addr);
        Command<K, V, String> cmd = dispatch(CLIENT, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String clientList() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(LIST);
        Command<K, V, String> cmd = dispatch(CLIENT, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<String> configGet(String parameter) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GET).add(parameter);
        Command<K, V, List<String>> cmd = dispatch(CONFIG, new StringListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String configResetstat() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(RESETSTAT);
        Command<K, V, String> cmd = dispatch(CONFIG, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String configSet(String parameter, String value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SET).add(parameter).add(value);
        Command<K, V, String> cmd = dispatch(CONFIG, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long dbsize() {
        Command<K, V, Long> cmd = dispatch(DBSIZE, new IntegerOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public String debugObject(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(OBJECT).addKey(key);
        Command<K, V, String> cmd = dispatch(DEBUG, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long decr(K key) {
        Command<K, V, Long> cmd = dispatch(DECR, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long decrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        Command<K, V, Long> cmd = dispatch(DECRBY, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long del(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, Long> cmd = dispatch(DEL, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String discard() {
        Command<K, V, String> cmd = dispatch(DISCARD, new StatusOutput<K, V>(codec));
        multi = null;
        return getOutput(cmd);
    }

    public V echo(V msg) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addValue(msg);
        Command<K, V, V> cmd = dispatch(ECHO, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean exists(K key) {
        Command<K, V, Boolean> cmd = dispatch(EXISTS, new BooleanOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Boolean expire(K key, long seconds) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds);
        Command<K, V, Boolean> cmd = dispatch(EXPIRE, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean expireat(K key, Date timestamp) {
        long seconds = timestamp.getTime() / 1000;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds);
        Command<K, V, Boolean> cmd = dispatch(EXPIREAT, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<Object> exec() {
        Command<K, V, List<Object>> cmd = dispatch(EXEC, multi);
        multi = null;
        return getOutput(cmd);
    }

    public String flushall() throws Exception {
        Command<K, V, String> cmd = dispatch(FLUSHALL, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public String flushdb() throws Exception {
        Command<K, V, String> cmd = dispatch(FLUSHDB, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public V get(K key) {
        Command<K, V, V> cmd = dispatch(GET, new ValueOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long getbit(K key, long offset) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset);
        Command<K, V, Long> cmd = dispatch(GETBIT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V getrange(K key, long start, long end) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(end);
        Command<K, V, V> cmd = dispatch(GETRANGE, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V getset(K key, V value) {
        Command<K, V, V> cmd = dispatch(GETSET, new ValueOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public Long hdel(K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        Command<K, V, Long> cmd = dispatch(HDEL, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean hexists(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        Command<K, V, Boolean> cmd = dispatch(HEXISTS, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V hget(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        Command<K, V, V> cmd = dispatch(HGET, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long hincrby(K key, K field, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).add(amount);
        Command<K, V, Long> cmd = dispatch(HINCRBY, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Map<K, V> hgetall(K key) {
        Command<K, V, Map<K, V>> cmd = dispatch(HGETALL, new MapOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public List<K> hkeys(K key) {
        Command<K, V, List<K>> cmd = dispatch(HKEYS, new KeyListOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long hlen(K key) {
        Command<K, V, Long> cmd = dispatch(HLEN, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public List<V> hmget(K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        Command<K, V, List<V>> cmd = dispatch(HMGET, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String hmset(K key, Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(map);
        Command<K, V, String> cmd = dispatch(HMSET, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean hset(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        Command<K, V, Boolean> cmd = dispatch(HSET, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean hsetnx(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        Command<K, V, Boolean> cmd = dispatch(HSETNX, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> hvals(K key) {
        Command<K, V, List<V>> cmd = dispatch(HVALS, new ValueListOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long incr(K key) {
        Command<K, V, Long> cmd = dispatch(INCR, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long incrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        Command<K, V, Long> cmd = dispatch(INCRBY, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String info() {
        Command<K, V, String> cmd = dispatch(INFO, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public List<K> keys(K pattern) {
        Command<K, V, List<K>> cmd = dispatch(KEYS, new KeyListOutput<K, V>(codec), pattern);
        return getOutput(cmd);
    }

    public Date lastsave() {
        Command<K, V, Date> cmd = dispatch(LASTSAVE, new DateOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public V lindex(K key, long index) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index);
        Command<K, V, V> cmd = dispatch(LINDEX, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long linsert(K key, boolean before, V pivot, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(before ? BEFORE : AFTER).addValue(pivot).addValue(value);
        Command<K, V, Long> cmd = dispatch(LINSERT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long llen(K key) {
        Command<K, V, Long> cmd = dispatch(LLEN, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public V lpop(K key) {
        Command<K, V, V> cmd = dispatch(LPOP, new ValueOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long lpush(K key, V... values) {
        Command<K, V, Long> cmd = dispatch(LPUSH, new IntegerOutput<K, V>(codec), key, values);
        return getOutput(cmd);
    }

    public Long lpushx(K key, V value) {
        Command<K, V, Long> cmd = dispatch(LPUSHX, new IntegerOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public List<V> lrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<K, V, List<V>> cmd = dispatch(LRANGE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long lrem(K key, long count, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(count).addValue(value);
        Command<K, V, Long> cmd = dispatch(LREM, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String lset(K key, long index, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index).addValue(value);
        Command<K, V, String> cmd = dispatch(LSET, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String ltrim(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<K, V, String> cmd = dispatch(LTRIM, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> mget(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, List<V>> cmd = dispatch(MGET, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean move(K key, int db) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(db);
        Command<K, V, Boolean> cmd = dispatch(MOVE, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String multi() {
        Command<K, V, String> cmd = dispatch(MULTI, new StatusOutput<K, V>(codec));
        String status = getOutput(cmd);
        if ("OK".equals(status)) {
            multi = new MultiOutput<K, V>(codec);
        }
        return status;
    }

    public String mset(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        Command<K, V, String> cmd = dispatch(MSET, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean msetnx(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        Command<K, V, Boolean> cmd = dispatch(MSETNX, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String objectEncoding(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(ENCODING).addKey(key);
        Command<K, V, String> cmd = dispatch(OBJECT, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long objectIdletime(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(IDLETIME).addKey(key);
        Command<K, V, Long> cmd = dispatch(OBJECT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long objectRefcount(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(REFCOUNT).addKey(key);
        Command<K, V, Long> cmd = dispatch(OBJECT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean persist(K key) {
        Command<K, V, Boolean> cmd = dispatch(PERSIST, new BooleanOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public String ping() {
        Command<K, V, String> cmd = dispatch(PING, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public Long publish(String channel, V message) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(channel).addValue(message);
        Command<K, V, Long> cmd = dispatch(PUBLISH, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String quit() {
        Command<K, V, String> cmd = dispatch(QUIT, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public V randomkey() {
        Command<K, V, V> cmd = dispatch(RANDOMKEY, new ValueOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public String rename(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        Command<K, V, String> cmd = dispatch(RENAME, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean renamenx(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        Command<K, V, Boolean> cmd = dispatch(RENAMENX, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V rpop(K key) {
        Command<K, V, V> cmd = dispatch(RPOP, new ValueOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public V rpoplpush(K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination);
        Command<K, V, V> cmd = dispatch(RPOPLPUSH, new ValueOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long rpush(K key, V... values) {
        Command<K, V, Long> cmd = dispatch(RPUSH, new IntegerOutput<K, V>(codec), key, values);
        return getOutput(cmd);
    }

    public Long rpushx(K key, V value) {
        Command<K, V, Long> cmd = dispatch(RPUSHX, new IntegerOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public Long sadd(K key, V... members) {
        Command<K, V, Long> cmd = dispatch(SADD, new IntegerOutput<K, V>(codec), key, members);
        return getOutput(cmd);
    }

    public String save() {
        Command<K, V, String> cmd = dispatch(SAVE, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public Long scard(K key) {
        Command<K, V, Long> cmd = dispatch(SCARD, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Set<V> sdiff(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, Set<V>> cmd = dispatch(SDIFF, new ValueSetOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long sdiffstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<K, V, Long> cmd = dispatch(SDIFFSTORE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String select(int db) {
        this.db = db;
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(db);
        Command<K, V, String> cmd = dispatch(SELECT, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String set(K key, V value) {
        Command<K, V, String> cmd = dispatch(SET, new StatusOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public Long setbit(K key, long offset, int value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).add(value);
        Command<K, V, Long> cmd = dispatch(SETBIT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String setex(K key, long seconds, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds).addValue(value);
        Command<K, V, String> cmd = dispatch(SETEX, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean setnx(K key, V value) {
        Command<K, V, Boolean> cmd = dispatch(SETNX, new BooleanOutput<K, V>(codec), key, value);
        return getOutput(cmd);
    }

    public Long setrange(K key, long offset, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).addValue(value);
        Command<K, V, Long> cmd = dispatch(SETRANGE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public void shutdown() {
        dispatch(SHUTDOWN, new StatusOutput<K, V>(codec));
    }

    public Set<V> sinter(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, Set<V>> cmd = dispatch(SINTER, new ValueSetOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long sinterstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<K, V, Long> cmd = dispatch(SINTERSTORE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Boolean sismember(K key, V member) {
        Command<K, V, Boolean> cmd = dispatch(SISMEMBER, new BooleanOutput<K, V>(codec), key, member);
        return getOutput(cmd);
    }

    public Boolean smove(K source, K destination, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination).addValue(member);
        Command<K, V, Boolean> cmd = dispatch(SMOVE, new BooleanOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String slaveof(String host, int port) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(host).add(port);
        Command<K, V, String> cmd = dispatch(SLAVEOF, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String slaveofNoOne() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NO).add(ONE);
        Command<K, V, String> cmd = dispatch(SLAVEOF, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Set<V> smembers(K key) {
        Command<K, V, Set<V>> cmd = dispatch(SMEMBERS, new ValueSetOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public List<V> sort(K key) {
        Command<K, V, List<V>> cmd = dispatch(SORT, new ValueListOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public List<V> sort(K key, SortArgs sortArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, null);
        Command<K, V, List<V>> cmd = dispatch(SORT, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long sortStore(K key, SortArgs sortArgs, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, destination);
        Command<K, V, Long> cmd = dispatch(SORT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public V spop(K key) {
        Command<K, V, V> cmd = dispatch(SPOP, new ValueOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public V srandmember(K key) {
        Command<K, V, V> cmd = dispatch(SRANDMEMBER, new ValueOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long srem(K key, V... members) {
        Command<K, V, Long> cmd = dispatch(SREM, new IntegerOutput<K, V>(codec), key, members);
        return getOutput(cmd);
    }

    public Set<V> sunion(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, Set<V>> cmd = dispatch(SUNION, new ValueSetOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long sunionstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        Command<K, V, Long> cmd = dispatch(SUNIONSTORE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String sync() {
        Command<K, V, String> cmd = dispatch(SYNC, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public Long strlen(K key) {
        Command<K, V, Long> cmd = dispatch(STRLEN, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long ttl(K key) {
        Command<K, V, Long> cmd = dispatch(TTL, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public String type(K key) {
        Command<K, V, String> cmd = dispatch(TYPE, new StatusOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public String watch(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        Command<K, V, String> cmd = dispatch(WATCH, new StatusOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public String unwatch() {
        Command<K, V, String> cmd = dispatch(UNWATCH, new StatusOutput<K, V>(codec));
        return getOutput(cmd);
    }

    public Long zadd(K key, double score, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(score).addValue(member);
        Command<K, V, Long> cmd = dispatch(ZADD, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    @SuppressWarnings("unchecked")
    public Long zadd(K key, Object... scoresAndValues) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        for (int i = 0; i < scoresAndValues.length; i += 2) {
            args.add((Double) scoresAndValues[i]);
            args.addValue((V) scoresAndValues[i + 1]);
        }
        Command<K, V, Long> cmd = dispatch(ZADD, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long zcard(K key) {
        Command<K, V, Long> cmd = dispatch(ZCARD, new IntegerOutput<K, V>(codec), key);
        return getOutput(cmd);
    }

    public Long zcount(K key, double min, double max) {
        return zcount(key, string(min), string(max));
    }

    public Long zcount(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<K, V, Long> cmd = dispatch(ZCOUNT, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Double zincrby(K key, double amount, K member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount).addKey(member);
        Command<K, V, Double> cmd = dispatch(ZINCRBY, new DoubleOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long zinterstore(K destination, K... keys) {
        return zinterstore(destination, new ZStoreArgs(), keys);
    }

    public Long zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        Command<K, V, Long> cmd = dispatch(ZINTERSTORE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<K, V, List<V>> cmd = dispatch(ZRANGE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZRANGE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrangebyscore(K key, double min, double max) {
        return zrangebyscore(key, string(min), string(max));
    }

    public List<V> zrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<K, V, List<V>> cmd = dispatch(ZRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrangebyscore(K key, double min, double max, long offset, long count) {
        return zrangebyscore(key, string(min), string(max), offset, count);
    }

    public List<V> zrangebyscore(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        Command<K, V, List<V>> cmd = dispatch(ZRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max) {
        return zrangebyscoreWithScores(key, string(min), string(max));
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return zrangebyscoreWithScores(key, string(min), string(max), offset, count);
    }

    public List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long zrank(K key, V member) {
        Command<K, V, Long> cmd = dispatch(ZRANK, new IntegerOutput<K, V>(codec), key, member);
        return getOutput(cmd);
    }

    public Long zrem(K key, V... members) {
        Command<K, V, Long> cmd = dispatch(ZREM, new IntegerOutput<K, V>(codec), key, members);
        return getOutput(cmd);
    }

    public Long zremrangebyrank(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<K, V, Long> cmd = dispatch(ZREMRANGEBYRANK, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long zremrangebyscore(K key, double min, double max) {
        return zremrangebyscore(key, string(min), string(max));
    }

    public Long zremrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        Command<K, V, Long> cmd = dispatch(ZREMRANGEBYSCORE, new IntegerOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        Command<K, V, List<V>> cmd = dispatch(ZREVRANGE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZREVRANGE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrangebyscore(K key, double max, double min) {
        return zrevrangebyscore(key, string(max), string(min));
    }

    public List<V> zrevrangebyscore(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(max).add(min);
        Command<K, V, List<V>> cmd = dispatch(ZREVRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<V> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return zrevrangebyscore(key, string(max), string(min), offset, count);
    }

    public List<V> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(LIMIT).add(offset).add(count);
        Command<K, V, List<V>> cmd = dispatch(ZREVRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return zrevrangebyscoreWithScores(key, string(max), string(min));
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZREVRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return zrevrangebyscoreWithScores(key, string(max), string(min), offset, count);
    }

    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        Command<K, V, List<ScoredValue<V>>> cmd = dispatch(ZREVRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
        return getOutput(cmd);
    }

    public Long zrevrank(K key, V member) {
        Command<K, V, Long> cmd = dispatch(ZREVRANK, new IntegerOutput<K, V>(codec), key, member);
        return getOutput(cmd);
    }

    public Double zscore(K key, V member) {
        Command<K, V, Double> cmd = dispatch(ZSCORE, new DoubleOutput<K, V>(codec), key, member);
        return getOutput(cmd);
    }

    public Long zunionstore(K destination, K... keys) {
        return zunionstore(destination, new ZStoreArgs(), keys);
    }

    public Long zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        Command<K, V, Long> cmd = dispatch(ZUNIONSTORE, new IntegerOutput<K, V>(codec), args);
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

        BlockingQueue<Command<K, V, ?>> tmp = new LinkedBlockingQueue<Command<K, V, ?>>();

        if (password != null) {
            CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(password);
            tmp.put(new Command<K, V, String>(AUTH, new StatusOutput<K, V>(codec), args));
        }

        if (db != 0) {
            CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(db);
            tmp.put(new Command<K, V, String>(SELECT, new StatusOutput<K, V>(codec), args));
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
            for (Command<K, V, ?> cmd : queue) {
                cmd.getOutput().setError("Connection closed");
                cmd.complete();
            }
            queue.clear();
            queue = null;
            channel = null;
        }
    }

    public <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output) {
        return dispatch(type, output, (CommandArgs<K, V>) null);
    }

    public <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output, K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return dispatch(type, output, args);
    }

    public <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output, K key, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(value);
        return dispatch(type, output, args);
    }

    public <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output, K key, V[] values) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValues(values);
        return dispatch(type, output, args);
    }

    public synchronized <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        Command<K, V, T> cmd = new Command<K, V, T>(type, output, args);

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

    public <T> T getOutput(Command<K, V, T> cmd) {
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
