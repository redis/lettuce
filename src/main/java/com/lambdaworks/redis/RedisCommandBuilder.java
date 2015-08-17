package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;
import static com.lambdaworks.redis.protocol.CommandType.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.ArrayOutput;
import com.lambdaworks.redis.output.BooleanListOutput;
import com.lambdaworks.redis.output.BooleanOutput;
import com.lambdaworks.redis.output.ByteArrayOutput;
import com.lambdaworks.redis.output.DateOutput;
import com.lambdaworks.redis.output.DoubleOutput;
import com.lambdaworks.redis.output.GeoCoordinatesListOutput;
import com.lambdaworks.redis.output.GeoWithinListOutput;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.output.KeyListOutput;
import com.lambdaworks.redis.output.KeyOutput;
import com.lambdaworks.redis.output.KeyScanOutput;
import com.lambdaworks.redis.output.KeyScanStreamingOutput;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyStreamingOutput;
import com.lambdaworks.redis.output.KeyValueOutput;
import com.lambdaworks.redis.output.KeyValueScanStreamingOutput;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingOutput;
import com.lambdaworks.redis.output.MapOutput;
import com.lambdaworks.redis.output.MapScanOutput;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.output.ScoredValueListOutput;
import com.lambdaworks.redis.output.ScoredValueScanOutput;
import com.lambdaworks.redis.output.ScoredValueScanStreamingOutput;
import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ScoredValueStreamingOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.output.StringListOutput;
import com.lambdaworks.redis.output.ValueListOutput;
import com.lambdaworks.redis.output.ValueOutput;
import com.lambdaworks.redis.output.ValueScanOutput;
import com.lambdaworks.redis.output.ValueScanStreamingOutput;
import com.lambdaworks.redis.output.ValueSetOutput;
import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.protocol.SetArgs;

/**
 * @param <K>
 * @param <V>
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class RedisCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    static final String MUST_NOT_CONTAIN_NULL_ELEMENTS = "must not contain null elements";
    static final String MUST_NOT_BE_EMPTY = "must not be empty";
    static final String MUST_NOT_BE_NULL = "must not be null";

    public RedisCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    public Command<K, V, Long> append(K key, V value) {
        return createCommand(APPEND, new IntegerOutput<K, V>(codec), key, value);
    }

    public Command<K, V, String> auth(String password) {
        assertNotEmpty(password, "password " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(OR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> bitopXor(K destination, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(XOR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, KeyValue<K, V>> blpop(long timeout, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys).add(timeout);
        return createCommand(BLPOP, new KeyValueOutput<K, V>(codec), args);
    }

    public Command<K, V, KeyValue<K, V>> brpop(long timeout, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(addr, "addr " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KILL).add(addr);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> clientKill(KillArgs killArgs) {
        assertNotNull(killArgs, "killArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KILL);
        killArgs.build(args);
        return createCommand(CLIENT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientPause(long timeout) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(PAUSE).add(timeout);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clientList() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(LIST);
        return createCommand(CLIENT, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<Object>> command() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        return createCommand(COMMAND, new ArrayOutput<K, V>(codec), args);
    }

    public Command<K, V, List<Object>> commandInfo(String... commands) {

        assertNotEmpty(commands, "commands " + MUST_NOT_BE_EMPTY);
        assertNoNullElements(commands, "commands " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(INFO);

        for (String command : commands) {
            args.add(command);
        }

        return createCommand(COMMAND, new ArrayOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> commandCount() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(COUNT);
        return createCommand(COMMAND, new IntegerOutput<K, V>(codec), args);
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

    public Command<K, V, Void> debugSegfault() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SEGFAULT);
        return createCommand(DEBUG, null, args);
    }

    public Command<K, V, Void> debugOom() {
        return createCommand(DEBUG, null, new CommandArgs<K, V>(codec).add("OOM"));
    }

    public Command<K, V, String> debugHtstats(int db) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(HTSTATS).add(db);
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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(script, "script " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(script).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    public <T> Command<K, V, T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        assertNotEmpty(script, "script " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(script).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    public <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K... keys) {
        assertNotEmpty(digest, "digest " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(digest).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    public <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        assertNotEmpty(digest, "digest " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.add(digest).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    public Command<K, V, Boolean> exists(K key) {
        return createCommand(EXISTS, new BooleanOutput<K, V>(codec), key);
    }

    public Command<K, V, Long> exists(K... keys) {
        return createCommand(EXISTS, new IntegerOutput<K, V>(codec), new CommandArgs<K, V>(codec).addKeys(keys));
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
        assertNotEmpty(fields, "fields " + MUST_NOT_BE_EMPTY);

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

    public Command<K, V, Long> hstrlen(K key, K field) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKey(field);
        return createCommand(HSTRLEN, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, List<V>> hmget(K key, K... fields) {
        assertNotEmpty(fields, "fields " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields) {
        assertNotEmpty(fields, "fields " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(values, "values " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(MGET, new ValueListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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

    public Command<K, V, String> readOnly() {
        return createCommand(READONLY, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, String> readWrite() {
        return createCommand(READWRITE, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> pttl(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return createCommand(PTTL, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> publish(K channel, V message) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(channel).addValue(message);
        return createCommand(PUBLISH, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, List<K>> pubsubChannels() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(CHANNELS);
        return createCommand(PUBSUB, new KeyListOutput<K, V>(codec), args);
    }

    public Command<K, V, List<K>> pubsubChannels(K pattern) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(CHANNELS).addKey(pattern);
        return createCommand(PUBSUB, new KeyListOutput<K, V>(codec), args);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Command<K, V, Map<K, Long>> pubsubNumsub(K... pattern) {
        assertNotEmpty(pattern, "pattern " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NUMSUB).addKeys(pattern);
        return createCommand(PUBSUB, (MapOutput) new MapOutput<K, Long>((RedisCodec) codec), args);
    }

    public Command<K, V, Long> pubsubNumpat() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NUMPAT);
        return createCommand(PUBSUB, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> quit() {
        return createCommand(QUIT, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, V> randomkey() {
        return createCommand(RANDOMKEY, new ValueOutput<K, V>(codec));
    }

    public Command<K, V, List<Object>> role() {
        return createCommand(ROLE, new ArrayOutput<K, V>(codec));
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
        assertNotEmpty(values, "values " + MUST_NOT_BE_EMPTY);

        return createCommand(RPUSH, new IntegerOutput<K, V>(codec), key, values);
    }

    public Command<K, V, Long> rpushx(K key, V value) {
        return createCommand(RPUSHX, new IntegerOutput<K, V>(codec), key, value);
    }

    public Command<K, V, Long> sadd(K key, V... members) {
        assertNotEmpty(members, "members " + MUST_NOT_BE_EMPTY);

        return createCommand(SADD, new IntegerOutput<K, V>(codec), key, members);
    }

    public Command<K, V, String> save() {
        return createCommand(SAVE, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> scard(K key) {
        return createCommand(SCARD, new IntegerOutput<K, V>(codec), key);
    }

    public Command<K, V, List<Boolean>> scriptExists(String... digests) {
        assertNotEmpty(digests, "digests " + MUST_NOT_BE_EMPTY);
        assertNoNullElements(digests, "digests " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(EXISTS);
        for (String sha : digests) {
            args.add(sha);
        }
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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sdiffstore(K destination, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sinterstore(K destination, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(host, "host " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(members, "members " + MUST_NOT_BE_EMPTY);

        return createCommand(SREM, new IntegerOutput<K, V>(codec), key, members);
    }

    public Command<K, V, Set<V>> sunion(K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueStreamingOutput<K, V>(codec, channel), args);
    }

    public Command<K, V, Long> sunionstore(K destination, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKeys(keys);
        return createCommand(WATCH, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> wait(int replicas, long timeout) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(replicas).add(timeout);

        return createCommand(WAIT, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> unwatch() {
        return createCommand(UNWATCH, new StatusOutput<K, V>(codec));
    }

    public Command<K, V, Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);

        if (zAddArgs != null) {
            zAddArgs.build(args);
        }
        args.add(score).addValue(member);

        return createCommand(ZADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Double> zaddincr(K key, double score, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        args.add(INCR);
        args.add(score).addValue(member);

        return createCommand(ZADD, new DoubleOutput<K, V>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        assertNotEmpty(scoresAndValues, "scoresAndValues " + MUST_NOT_BE_EMPTY);
        assertNoNullElements(scoresAndValues, "scoresAndValues " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        assertTrue(scoresAndValues.length % 2 == 0, "scoresAndValues.length must be a multiple of 2 and contain a "
                + "sequence of score1, value1, score2, value2, scoreN,valueN");

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);

        if (zAddArgs != null) {
            zAddArgs.build(args);
        }

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        return zinterstore(destination, new ZStoreArgs(), keys);
    }

    public Command<K, V, Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(members, "members " + MUST_NOT_BE_EMPTY);

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
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        return zunionstore(destination, new ZStoreArgs(), keys);
    }

    public Command<K, V, Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        assertNotEmpty(keys, "keys " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        return createCommand(ZUNIONSTORE, new IntegerOutput<K, V>(codec), args);
    }

    public RedisCommand<K, V, Long> zlexcount(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZLEXCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    public RedisCommand<K, V, Long> zremrangebylex(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZREMRANGEBYLEX, new IntegerOutput<K, V>(codec), args);
    }

    public RedisCommand<K, V, List<V>> zrangebylex(K key, String min, String max) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYLEX, new ValueListOutput<K, V>(codec), args);
    }

    public RedisCommand<K, V, List<V>> zrangebylex(K key, String min, String max, long offset, long count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYLEX, new ValueListOutput<K, V>(codec), args);
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
        assertNotNull(value, "value " + MUST_NOT_BE_NULL);
        assertNotNull(moreValues, "moreValues " + MUST_NOT_BE_NULL);
        assertNoNullElements(moreValues, "moreValues " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(value).addValues(moreValues);
        return createCommand(PFADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> pfcount(K key, K... moreKeys) {
        assertNotNull(key, "key " + MUST_NOT_BE_NULL);
        assertNotNull(moreKeys, "moreKeys " + MUST_NOT_BE_NULL);
        assertNoNullElements(moreKeys, "moreKeys " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addKeys(moreKeys);
        return createCommand(PFCOUNT, new IntegerOutput<K, V>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> pfmerge(K destkey, K sourcekey, K... moreSourceKeys) {
        assertNotNull(destkey, "destkey " + MUST_NOT_BE_NULL);
        assertNotNull(sourcekey, "sourcekey " + MUST_NOT_BE_NULL);
        assertNotNull(moreSourceKeys, "moreSourceKeys " + MUST_NOT_BE_NULL);
        assertNoNullElements(moreSourceKeys, "moreSourceKeys " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

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
        assertNotEmpty(slots, "slots " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(ADDSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterDelslots(int[] slots) {
        assertNotEmpty(slots, "slots " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(DELSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterInfo() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(INFO);

        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterMyId() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(MYID);

        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterNodes() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(NODES);

        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<K>> clusterGetKeysInSlot(int slot, int count) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(GETKEYSINSLOT).add(slot).add(count);
        return createCommand(CLUSTER, new KeyListOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> clusterCountKeysInSlot(int slot) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(COUNTKEYSINSLOT).add(slot);
        return createCommand(CLUSTER, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> clusterCountFailureReports(String nodeId) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add("COUNT-FAILURE-REPORTS").add(nodeId);
        return createCommand(CLUSTER, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Long> clusterKeyslot(K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(KEYSLOT).addKey(key);
        return createCommand(CLUSTER, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterSaveconfig() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SAVECONFIG);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, String> clusterSetConfigEpoch(long configEpoch) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add("SET-CONFIG-EPOCH").add(configEpoch);
        return createCommand(CLUSTER, new StatusOutput<K, V>(codec), args);
    }

    public Command<K, V, List<Object>> clusterSlots() {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SLOTS);
        return createCommand(CLUSTER, new ArrayOutput<K, V>(codec), args);
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

    public Command<K, V, List<String>> clusterSlaves(String nodeId) {

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).add(SLAVES).add(nodeId);
        return createCommand(CLUSTER, new StringListOutput<K, V>(codec), args);
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

    public Command<K, V, Long> geoadd(K key, double longitude, double latitude, V member) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(longitude).add(latitude).addValue(member);
        return createCommand(GEOADD, new IntegerOutput<K, V>(codec), args);
    }

    @SuppressWarnings("unchecked")
    public Command<K, V, Long> geoadd(K key, Object[] lngLatMember) {

        assertNotEmpty(lngLatMember, "lngLatMember " + MUST_NOT_BE_EMPTY);
        assertNoNullElements(lngLatMember, "lngLatMember " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        assertTrue(
                lngLatMember.length % 3 == 0,
                "lngLatMember.length must be a multiple of 3 and contain a "
                        + "sequence of longitude1, latitude1, member1, longitude2, latitude2, member2, ... longitudeN, latitudeN, memberN");

        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);

        for (int i = 0; i < lngLatMember.length; i += 3) {
            args.add((Double) lngLatMember[i]);
            args.add((Double) lngLatMember[i + 1]);
            args.addValue((V) lngLatMember[i + 2]);
        }

        return createCommand(GEOADD, new IntegerOutput<K, V>(codec), args);
    }

    public Command<K, V, Set<V>> georadius(K key, double longitude, double latitude, double distance, String unit) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(longitude).add(latitude).add(distance).add(unit);
        return createCommand(GEORADIUS, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, List<GeoWithin<V>>> georadius(K key, double longitude, double latitude, double distance, String unit,
            GeoArgs geoArgs) {

        assertNotNull(geoArgs, "geoArgs must not be null");
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).add(longitude).add(latitude).add(distance).add(unit);
        geoArgs.build(args);

        return createCommand(GEORADIUS, new GeoWithinListOutput<K, V>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(),
                geoArgs.isWithCoordinates()), args);
    }

    public Command<K, V, Set<V>> georadiusbymember(K key, V member, double distance, String unit) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(member).add(distance).add(unit);
        return createCommand(GEORADIUSBYMEMBER, new ValueSetOutput<K, V>(codec), args);
    }

    public Command<K, V, List<GeoWithin<V>>> georadiusbymember(K key, V member, double distance, String unit, GeoArgs geoArgs) {
        assertNotNull(geoArgs, "geoArgs must not be null");
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(member).add(distance).add(unit);
        geoArgs.build(args);

        return createCommand(
                GEORADIUSBYMEMBER,
                new GeoWithinListOutput<K, V>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(), geoArgs
                        .isWithCoordinates()), args);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Command<K, V, List<GeoCoordinates>> geopos(K key, V[] members) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValues(members);

        return (Command) createCommand(GEOPOS, new GeoCoordinatesListOutput<K, V>(codec), args);
    }

    public Command<K, V, Double> geodist(K key, V from, V to, GeoArgs.Unit unit) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(from).addValue(to);

        if (unit != null) {
            args.add(unit.name());
        }

        return createCommand(GEODIST, new DoubleOutput<K, V>(codec), args);
    }

    /**
     * Assert that a string is not empty, it must not be {@code null} and it must not be empty.
     *
     * @param string the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null} or the underlying string is empty
     */
    protected static void assertNotEmpty(String string, String message) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an object is not {@code null} .
     *
     * @param object the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null}
     */
    public static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    protected void assertNotEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    protected static void assertNotEmpty(int[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has no null elements.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void assertNoNullElements(Object[] array, String message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Assert that {@code value} is {@literal true}.
     *
     * @param value the value to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }

}
