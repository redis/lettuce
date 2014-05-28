package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;
import static com.lambdaworks.redis.protocol.CommandType.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.*;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.SetArgs;

/**
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <K>
 * @param <V>
 */
class RedisCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    public RedisCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    public Command<K, V, Long> append(K key, V value) {
        return createCommand(APPEND, new IntegerOutput<K, V>(codec), key, value);
    }

    public Command<K, V, String> auth(String password) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(password);
        return createCommand(AUTH, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> bgrewriteaof() {
        return createCommand(BGREWRITEAOF, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> bgsave() {
        return createCommand(BGSAVE, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> bitcount(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return createCommand(BITCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitcount(K key, long start, long end) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(end);
        return createCommand(BITCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitpos(K key, boolean state) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(state ? 1 : 0);
        return createCommand(BITPOS, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitpos(K key, boolean state, long start, long end) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(state ? 1 : 0).add(start).add(end);
        return createCommand(BITPOS, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitopAnd(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(AND).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitopNot(K destination, K source) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(NOT).addKey(destination).addKey(source);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitopOr(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(OR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitopXor(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(XOR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, KeyValue<K, V>> blpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        return createCommand(BLPOP, new KeyValueOutput<K, V>(codec), args);
    }

    public Command<K, V, KeyValue<K, V>> brpop(long timeout, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        return createCommand(BRPOP, new KeyValueOutput<K, V>(codec), args);
    }

    public Command<K, V, V> brpoplpush(long timeout, K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(source).addKey(destination).add(timeout);
        return createCommand(BRPOPLPUSH, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, K> clientGetname() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GETNAME);
        return createCommand(CLIENT, new KeyOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientSetname(K name) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SETNAME).addKey(name);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientKill(String addr) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KILL).add(addr);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientPause(long timeout) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(PAUSE).add(timeout);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientList() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(LIST);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> configRewrite() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(REWRITE);
        return createCommand(CONFIG, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<String>> configGet(String parameter) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GET).add(parameter);
        return createCommand(CONFIG, new StringListOutput<K, V>(codec), args);
    }

    public Command<K, V, String> configResetstat() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(RESETSTAT);
        return createCommand(CONFIG, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> configSet(String parameter, String value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SET).add(parameter).add(value);
        return createCommand(CONFIG, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> dbsize() {
        return createCommand(DBSIZE, new IntegerOutput<K, V>(codec));
    }

    public Command<K, V, String> debugObject(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(OBJECT).addKey(key);
        return createCommand(DEBUG, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> decr(K key) {
        return createCommand(DECR, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> decrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        return createCommand(DECRBY, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> del(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(DEL, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> discard() {
        return createCommand(DISCARD, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, byte[]> dump(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return createCommand(DUMP, new ByteArrayOutput<K, V>(codec), args);
    }

    public Command<K, V, V> echo(V msg) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addValue(msg);
        return createCommand(ECHO, new ValueOutput<K, V>(codec), args);
    }

    public <T> Command<K, V, T> eval(String script, ScriptOutputType type, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(script).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    public <T> Command<K, V, T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(script).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    public <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(digest).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    public <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(digest).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    public Command<K, V, Boolean> exists(K key) {
        return createCommand(EXISTS, new BooleanOutput<K, V>(codec), key);
    }

    public Command<K, V, Boolean> expire(K key, long seconds) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds);
        return createCommand(EXPIRE, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> expireat(K key, long timestamp) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(timestamp);
        return createCommand(EXPIREAT, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> flushall() {
        return createCommand(FLUSHALL, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> flushdb() {
        return createCommand(FLUSHDB, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, V> get(K key) {
        return createCommand(GET, new ValueOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> getbit(K key, long offset) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset);
        return createCommand(GETBIT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, V> getrange(K key, long start, long end) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(end);
        return createCommand(GETRANGE, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, V> getset(K key, V value) {
        return createCommand(GETSET, new ValueOutput<K, V>(codec), key, value);
    }

    public Command<K, V, Long> hdel(K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        return createCommand(HDEL, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> hexists(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        return createCommand(HEXISTS, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, V> hget(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        return createCommand(HGET, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> hincrby(K key, K field, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).add(amount);
        return createCommand(HINCRBY, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Double> hincrbyfloat(K key, K field, double amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).add(amount);
        return createCommand(HINCRBYFLOAT, new DoubleOutput<K, V>(codec), args);
    }

    public Command<K, V, Map<K, V>> hgetall(K key) {
        return createCommand(HGETALL, new MapOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        return createCommand(HGETALL, new KeyValueStreamingOutput<K, V>(codec, channel), key);
    }

    public Command<K, V, List<K>> hkeys(K key) {
        return createCommand(HKEYS, new KeyListOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        return createCommand(HKEYS, new KeyStreamingOutput<K, V>(codec, channel), key);
    }

    public Command<K, V, Long> hlen(K key) {
        return createCommand(HLEN, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, List<V>> hmget(K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, String> hmset(K key, Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(map);
        return createCommand(HMSET, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> hset(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        return createCommand(HSET, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> hsetnx(K key, K field, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field).addValue(value);
        return createCommand(HSETNX, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> hvals(K key) {
        return createCommand(HVALS, new ValueListOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> hvals(ValueStreamingChannel<V> channel, K key) {
        return createCommand(HVALS, new ValueStreamingOutput<K, V>(codec, channel), key);
    }

    public Command<K, V, Long> incr(K key) {
        return createCommand(INCR, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> incrby(K key, long amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        return createCommand(INCRBY, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Double> incrbyfloat(K key, double amount) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount);
        return createCommand(INCRBYFLOAT, new DoubleOutput<K, V>(codec), args);
    }

    public Command<K, V, String> info() {
        return createCommand(INFO, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> info(String section) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(section);
        return createCommand(INFO, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<K>> keys(K pattern) {
        return createCommand(KEYS, new KeyListOutput<K, V>(codec), pattern);
    }

    public Command<K, V, Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        return createCommand(KEYS, new KeyStreamingOutput<K, V>(codec, channel), pattern);
    }

    public Command<K, V, Date> lastsave() {
        return createCommand(LASTSAVE, new DateOutput<K, V>(codec));
    }

    public Command<K, V, V> lindex(K key, long index) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index);
        return createCommand(LINDEX, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> linsert(K key, boolean before, V pivot, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(before ? BEFORE : AFTER).addValue(pivot).addValue(value);
        return createCommand(LINSERT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> llen(K key) {
        return createCommand(LLEN, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, V> lpop(K key) {
        return createCommand(LPOP, new ValueOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> lpush(K key, V... values) {
        return createCommand(LPUSH, new IntegerOutput<K, V>(codec), key, values);
    }

    public Command<K, V, Long> lpushx(K key, V value) {
        return createCommand(LPUSHX, new IntegerOutput<K, V>(codec), key, value);
    }

    public Command<K, V, List<V>> lrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(LRANGE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(LRANGE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> lrem(K key, long count, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(count).addValue(value);
        return createCommand(LREM, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> lset(K key, long index, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(index).addValue(value);
        return createCommand(LSET, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> ltrim(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(LTRIM, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> migrate(String host, int port, K key, int db, long timeout) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(host).add(port).addKey(key).add(db).add(timeout);
        return createCommand(MIGRATE, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> mget(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(MGET, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(MGET, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Boolean> move(K key, int db) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(db);
        return createCommand(MOVE, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> multi() {
        return createCommand(MULTI, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> mset(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        return createCommand(MSET, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> msetnx(Map<K, V> map) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(map);
        return createCommand(MSETNX, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> objectEncoding(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(ENCODING).addKey(key);
        return createCommand(OBJECT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> objectIdletime(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(IDLETIME).addKey(key);
        return createCommand(OBJECT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> objectRefcount(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(REFCOUNT).addKey(key);
        return createCommand(OBJECT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> persist(K key) {
        return createCommand(PERSIST, new BooleanOutput<K, V>(codec), key);
    }

    public Command<K, V, Boolean> pexpire(K key, long milliseconds) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(milliseconds);
        return createCommand(PEXPIRE, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> pexpireat(K key, long timestamp) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(timestamp);
        return createCommand(PEXPIREAT, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> ping() {
        return createCommand(PING, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> pttl(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return createCommand(PTTL, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> publish(K channel, V message) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(channel).addValue(message);
        return createCommand(PUBLISH, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> quit() {
        return createCommand(QUIT, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, V> randomkey() {
        return createCommand(RANDOMKEY, new ValueOutput<K, V>(codec));
    }

    public Command<K, V, String> rename(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        return createCommand(RENAME, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> renamenx(K key, K newKey) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(newKey);
        return createCommand(RENAMENX, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> restore(K key, long ttl, byte[] value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(ttl).add(value);
        return createCommand(RESTORE, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, V> rpop(K key) {
        return createCommand(RPOP, new ValueOutput<K, V>(codec), key);
    }

    public Command<K, V, V> rpoplpush(K source, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination);
        return createCommand(RPOPLPUSH, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> rpush(K key, V... values) {
        return createCommand(RPUSH, new IntegerOutput<K, V>(codec), key, values);
    }

    public Command<K, V, Long> rpushx(K key, V value) {
        return createCommand(RPUSHX, new IntegerOutput<K, V>(codec), key, value);
    }

    public Command<K, V, Long> sadd(K key, V... members) {
        return createCommand(SADD, new IntegerOutput<K, V>(codec), key, members);
    }

    public Command<K, V, String> save() {
        return createCommand(SAVE, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> scard(K key) {
        return createCommand(SCARD, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, List<Boolean>> scriptExists(String... digests) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(EXISTS);
        for (String sha : digests)
            args.add(sha);
        return createCommand(SCRIPT, new BooleanListOutput<K, V>(codec), args);
    }

    public Command<K, V, String> scriptFlush() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(FLUSH);
        return createCommand(SCRIPT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> scriptKill() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KILL);
        return createCommand(SCRIPT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> scriptLoad(V script) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(LOAD).addValue(script);
        return createCommand(SCRIPT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Set<V>> sdiff(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sdiffstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        return createCommand(SDIFFSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> select(int db) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(db);
        return createCommand(SELECT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> set(K key, V value) {
        return createCommand(SET, new StatusOutput<K, V>(codec), key, value);
    }

    public Command<K, V, V> set(K key, V value, SetArgs setArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(value);
        setArgs.build(args);
        return createCommand(SET, new ValueOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> setbit(K key, long offset, int value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).add(value);
        return createCommand(SETBIT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> setex(K key, long seconds, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(seconds).addValue(value);
        return createCommand(SETEX, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> psetex(K key, long milliseconds, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(milliseconds).addValue(value);
        return createCommand(PSETEX, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> setnx(K key, V value) {
        return createCommand(SETNX, new BooleanOutput<K, V>(codec), key, value);
    }

    public Command<K, V, Long> setrange(K key, long offset, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(offset).addValue(value);
        return createCommand(SETRANGE, new IntegerOutput<K, V>(codec), args);
    }

    @Deprecated
    public Command<K, V, String> shutdown() {
        return createCommand(SHUTDOWN, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> shutdown(boolean save) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        return createCommand(SHUTDOWN, new StatusOutput<K, V>(codec), save ? args.add(SAVE) : args.add(NOSAVE));
    }

    public Command<K, V, Set<V>> sinter(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sinterstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        return createCommand(SINTERSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Boolean> sismember(K key, V member) {
        return createCommand(SISMEMBER, new BooleanOutput<K, V>(codec), key, member);
    }

    public Command<K, V, Boolean> smove(K source, K destination, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(source).addKey(destination).addValue(member);
        return createCommand(SMOVE, new BooleanOutput<K, V>(codec), args);
    }

    public Command<K, V, String> slaveof(String host, int port) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(host).add(port);
        return createCommand(SLAVEOF, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> slaveofNoOne() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NO).add(ONE);
        return createCommand(SLAVEOF, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<Object>> slowlogGet() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GET);
        return createCommand(SLOWLOG, new NestedMultiOutput<K, V>(codec), args);
    }

    public Command<K, V, List<Object>> slowlogGet(int count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GET).add(count);
        return createCommand(SLOWLOG, new NestedMultiOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> slowlogLen() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(LEN);
        return createCommand(SLOWLOG, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> slowlogReset() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(RESET);
        return createCommand(SLOWLOG, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Set<V>> smembers(K key) {
        return createCommand(SMEMBERS, new ValueSetOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> smembers(ValueStreamingChannel<V> channel, K key) {
        return createCommand(SMEMBERS, new ValueStreamingOutput<K, V>(codec, channel), key);
    }

    public Command<K, V, List<V>> sort(K key) {
        return createCommand(SORT, new ValueListOutput<K, V>(codec), key);
    }

    public Command<K, V, List<V>> sort(K key, SortArgs sortArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sort(ValueStreamingChannel<V> channel, K key) {
        return createCommand(SORT, new ValueStreamingOutput<K, V>(codec, channel), key);
    }

    public Command<K, V, Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sortStore(K key, SortArgs sortArgs, K destination) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        sortArgs.build(args, destination);
        return createCommand(SORT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, V> spop(K key) {
        return createCommand(SPOP, new ValueOutput<K, V>(codec), key);
    }

    public Command<K, V, V> srandmember(K key) {
        return createCommand(SRANDMEMBER, new ValueOutput<K, V>(codec), key);
    }

    public Command<K, V, Set<V>> srandmember(K key, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(count);
        return createCommand(SRANDMEMBER, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(count);
        return createCommand(SRANDMEMBER, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> srem(K key, V... members) {
        return createCommand(SREM, new IntegerOutput<K, V>(codec), key, members);
    }

    public Command<K, V, Set<V>> sunion(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sunionstore(K destination, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).addKeys(keys);
        return createCommand(SUNIONSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> sync() {
        return createCommand(SYNC, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> strlen(K key) {
        return createCommand(STRLEN, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> ttl(K key) {
        return createCommand(TTL, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, String> type(K key) {
        return createCommand(TYPE, new StatusOutput<K, V>(codec), key);
    }

    public Command<K, V, String> watch(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(WATCH, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> unwatch() {
        return createCommand(UNWATCH, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> zadd(K key, double score, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(score).addValue(member);
        return createCommand(ZADD, new IntegerOutput<K, V>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> zadd(K key, Object... scoresAndValues) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        for (int i = 0; i < scoresAndValues.length; i += 2) {
            args.add((Double) scoresAndValues[i]);
            args.addValue((V) scoresAndValues[i + 1]);
        }
        return createCommand(ZADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> zcard(K key) {
        return createCommand(ZCARD, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> zcount(K key, double min, double max) {
        return zcount(key, string(min), string(max));
    }

    public Command<K, V, Long> zcount(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        return createCommand(ZCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Double> zincrby(K key, double amount, K member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(amount).addKey(member);
        return createCommand(ZINCRBY, new DoubleOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> zinterstore(K destination, K... keys) {
        return zinterstore(destination, new ZStoreArgs(), keys);
    }

    public Command<K, V, Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        return createCommand(ZINTERSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZRANGE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZRANGE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrangebyscore(K key, double min, double max) {
        return zrangebyscore(key, string(min), string(max));
    }

    public Command<K, V, List<V>> zrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrangebyscore(K key, double min, double max, long offset, long count) {
        return zrangebyscore(key, string(min), string(max), offset, count);
    }

    public Command<K, V, List<V>> zrangebyscore(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max) {
        return zrangebyscoreWithScores(key, string(min), string(max));
    }

    public Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        return createCommand(ZRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return zrangebyscoreWithScores(key, string(min), string(max), offset, count);
    }

    public Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZRANGE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZRANGE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return zrangebyscore(channel, key, string(min), string(max));
    }

    public Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYSCORE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset,
            long count) {
        return zrangebyscore(channel, key, string(min), string(max), offset, count);
    }

    public Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset,
            long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYSCORE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return zrangebyscoreWithScores(channel, key, string(min), string(max));
    }

    public Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return zrangebyscoreWithScores(channel, key, string(min), string(max), offset, count);
    }

    public Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrank(K key, V member) {
        return createCommand(ZRANK, new IntegerOutput<K, V>(codec), key, member);
    }

    public Command<K, V, Long> zrem(K key, V... members) {
        return createCommand(ZREM, new IntegerOutput<K, V>(codec), key, members);
    }

    public Command<K, V, Long> zremrangebyrank(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREMRANGEBYRANK, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> zremrangebyscore(K key, double min, double max) {
        return zremrangebyscore(key, string(min), string(max));
    }

    public Command<K, V, Long> zremrangebyscore(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(min).add(max);
        return createCommand(ZREMRANGEBYSCORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrevrange(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREVRANGE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZREVRANGE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrevrangebyscore(K key, double max, double min) {
        return zrevrangebyscore(key, string(max), string(min));
    }

    public Command<K, V, List<V>> zrevrangebyscore(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(max).add(min);
        return createCommand(ZREVRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return zrevrangebyscore(key, string(max), string(min), offset, count);
    }

    public Command<K, V, List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(LIMIT).add(offset).add(count);
        return createCommand(ZREVRANGEBYSCORE, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return zrevrangebyscoreWithScores(key, string(max), string(min));
    }

    public Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return zrevrangebyscoreWithScores(key, string(max), string(min), offset, count);
    }

    public Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREVRANGE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZREVRANGE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return zrevrangebyscore(channel, key, string(max), string(min));
    }

    public Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(max).add(min);
        return createCommand(ZREVRANGEBYSCORE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return zrevrangebyscore(channel, key, string(max), string(min), offset, count);
    }

    public Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(LIMIT).add(offset).add(count);
        return createCommand(ZREVRANGEBYSCORE, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return zrevrangebyscoreWithScores(channel, key, string(max), string(min));
    }

    public Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max,
            double min, long offset, long count) {
        return zrevrangebyscoreWithScores(channel, key, string(max), string(min), offset, count);
    }

    public Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max,
            String min, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES).add(LIMIT).add(offset).add(count);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> zrevrank(K key, V member) {
        return createCommand(ZREVRANK, new IntegerOutput<K, V>(codec), key, member);
    }

    public Command<K, V, Double> zscore(K key, V member) {
        return createCommand(ZSCORE, new DoubleOutput<K, V>(codec), key, member);
    }

    public Command<K, V, Long> zunionstore(K destination, K... keys) {
        return zunionstore(destination, new ZStoreArgs(), keys);
    }

    public Command<K, V, Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        return createCommand(ZUNIONSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> time() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        return createCommand(TIME, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, KeyScanCursor<K>> scan() {
        return scan(null, null);
    }

    public Command<K, V, KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return scan(scanCursor, null);
    }

    public Command<K, V, KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return scan(null, scanArgs);
    }

    public Command<K, V, KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);

        scanArgs(scanCursor, scanArgs, args);

        KeyScanOutput<K, V> output = new KeyScanOutput<K, V>(codec);
        return createCommand(SCAN, output, args);
    }

    protected void scanArgs(ScanCursor scanCursor, ScanArgs scanArgs, CommandArgs<K, V> args) {
        if (scanCursor != null) {
            args.add(scanCursor.getCursor());
        } else {
            args.add("0");
        }

        if (scanArgs != null) {
            scanArgs.build(args);
        }
    }

    public Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel) {
        return scanStreaming(channel, null, null);
    }

    public Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return scanStreaming(channel, scanCursor, null);
    }

    public Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return scanStreaming(channel, null, scanArgs);
    }

    public Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        scanArgs(scanCursor, scanArgs, args);

        KeyScanStreamingOutput<K, V> output = new KeyScanStreamingOutput<K, V>(codec, channel);
        return createCommand(SCAN, output, args);
    }

    public Command<K, V, ValueScanCursor<V>> sscan(K key) {
        return sscan(key, null, null);
    }

    public Command<K, V, ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return sscan(key, scanCursor, null);
    }

    public Command<K, V, ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return sscan(key, null, scanArgs);
    }

    public Command<K, V, ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        ValueScanOutput<K, V> output = new ValueScanOutput<K, V>(codec);
        return createCommand(SSCAN, output, args);
    }

    public Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key) {
        return sscanStreaming(channel, key, null, null);
    }

    public Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return sscanStreaming(channel, key, scanCursor, null);
    }

    public Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return sscanStreaming(channel, key, null, scanArgs);
    }

    public Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        ValueScanStreamingOutput<K, V> output = new ValueScanStreamingOutput<K, V>(codec, channel);
        return createCommand(SSCAN, output, args);
    }

    public Command<K, V, MapScanCursor<K, V>> hscan(K key) {
        return hscan(key, null, null);
    }

    public Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return hscan(key, scanCursor, null);
    }

    public Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return hscan(key, null, scanArgs);
    }

    public Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        MapScanOutput<K, V> output = new MapScanOutput<K, V>(codec);
        return createCommand(HSCAN, output, args);
    }

    public Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key) {
        return hscanStreaming(channel, key, null, null);
    }

    public Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return hscanStreaming(channel, key, scanCursor, null);
    }

    public Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return hscanStreaming(channel, key, null, scanArgs);
    }

    public Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        KeyValueScanStreamingOutput<K, V> output = new KeyValueScanStreamingOutput<K, V>(codec, channel);
        return createCommand(HSCAN, output, args);
    }

    public Command<K, V, ScoredValueScanCursor<V>> zscan(K key) {
        return zscan(key, null, null);
    }

    public Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return zscan(key, scanCursor, null);
    }

    public Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return zscan(key, null, scanArgs);
    }

    public Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        ScoredValueScanOutput<K, V> output = new ScoredValueScanOutput<K, V>(codec);
        return createCommand(ZSCAN, output, args);
    }

    public Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key) {
        return zscanStreaming(channel, key, null, null);
    }

    public Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return zscanStreaming(channel, key, scanCursor, null);
    }

    public Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return zscanStreaming(channel, key, null, scanArgs);
    }

    public Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        ScoredValueScanStreamingOutput<K, V> output = new ScoredValueScanStreamingOutput<K, V>(codec, channel);
        return createCommand(ZSCAN, output, args);
    }

    public Command<K, V, Long> pfadd(K key, V value, V... moreValues) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(value).addValues(moreValues);
        return createCommand(PFADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> pfcount(K key, K... moreKeys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(moreKeys);
        return createCommand(PFCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> pfmerge(K destkey, K sourcekey, K... moreSourceKeys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(destkey).addKey(sourcekey).addKeys(moreSourceKeys);
        return createCommand(PFADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterMeet(String ip, int port) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(MEET).add(ip).add(port);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterForget(String nodeId) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(FORGET).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterAddslots(int[] slots) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(ADDSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterDelslots(int[] slots) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(DELSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<String>> clusterInfo() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(INFO);

        return createCommand(CLUSTER, new StringListOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterNodes() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NODES);

        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<K>> clusterGetKeysInSlot(int slot, int count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GETKEYSINSLOT).add(slot).add(count);
        return createCommand(CLUSTER, new KeyListOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterSetSlotNode(int slot, String nodeId) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SETSLOT).add(slot).add(NODE).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterSetSlotMigrating(int slot, String nodeId) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SETSLOT).add(slot).add(MIGRATING).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterSetSlotImporting(int slot, String nodeId) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SETSLOT).add(slot).add(IMPORTING).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterReplicate(String nodeId) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(REPLICATE).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> asking() {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        return createCommand(ASKING, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterFlushslots() {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(FLUSHSLOTS);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterFailover(boolean force) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(FAILOVER);
        if (force) {
            args.add(FORCE);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterReset(boolean hard) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(RESET);
        if (hard) {
            args.add(HARD);
        } else {
            args.add(SOFT);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }
}
