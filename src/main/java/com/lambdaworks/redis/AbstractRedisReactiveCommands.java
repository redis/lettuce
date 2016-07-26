package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandType.EXEC;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.*;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.Subscriber;

import com.lambdaworks.redis.GeoArgs.Unit;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.rx.*;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.*;

/**
 * A reactive and thread-safe API for a Redis connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public abstract class AbstractRedisReactiveCommands<K, V> implements RedisHashReactiveCommands<K, V>,
        RedisKeyReactiveCommands<K, V>, RedisStringReactiveCommands<K, V>, RedisListReactiveCommands<K, V>,
        RedisSetReactiveCommands<K, V>, RedisSortedSetReactiveCommands<K, V>, RedisScriptingReactiveCommands<K, V>,
        RedisServerReactiveCommands<K, V>, RedisHLLReactiveCommands<K, V>, BaseRedisReactiveCommands<K, V>,
        RedisTransactionalReactiveCommands<K, V>, RedisGeoReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    protected MultiOutput<K, V> multi;
    protected RedisCommandBuilder<K, V> commandBuilder;
    protected RedisCodec<K, V> codec;
    protected StatefulConnection<K, V> connection;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     */
    public AbstractRedisReactiveCommands(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        this.codec = codec;
        commandBuilder = new RedisCommandBuilder<K, V>(codec);
    }

    @Override
    public Single<Long> append(K key, V value) {
        return createSingle(() -> commandBuilder.append(key, value));
    }

    @Override
    public Single<String> auth(String password) {
        return createSingle(() -> commandBuilder.auth(password));

    }

    @Override
    public Single<String> bgrewriteaof() {
        return createSingle(commandBuilder::bgrewriteaof);
    }

    @Override
    public Single<String> bgsave() {
        return createSingle(commandBuilder::bgsave);
    }

    @Override
    public Single<Long> bitcount(K key) {
        return createSingle(() -> commandBuilder.bitcount(key));
    }

    @Override
    public Single<Long> bitcount(K key, long start, long end) {
        return createSingle(() -> commandBuilder.bitcount(key, start, end));
    }

    @Override
    public Observable<Long> bitfield(K key, BitFieldArgs args) {
        return createDissolvingObservable(() -> commandBuilder.bitfield(key, args));
    }

    @Override
    public Single<Long> bitpos(K key, boolean state) {
        return createSingle(() -> commandBuilder.bitpos(key, state));
    }

    @Override
    public Single<Long> bitpos(K key, boolean state, long start, long end) {
        return createSingle(() -> commandBuilder.bitpos(key, state, start, end));
    }

    @Override
    public Single<Long> bitopAnd(K destination, K... keys) {
        return createSingle(() -> commandBuilder.bitopAnd(destination, keys));
    }

    @Override
    public Single<Long> bitopNot(K destination, K source) {
        return createSingle(() -> commandBuilder.bitopNot(destination, source));
    }

    @Override
    public Single<Long> bitopOr(K destination, K... keys) {
        return createSingle(() -> commandBuilder.bitopOr(destination, keys));
    }

    @Override
    public Single<Long> bitopXor(K destination, K... keys) {
        return createSingle(() -> commandBuilder.bitopXor(destination, keys));
    }

    @Override
    public Single<KeyValue<K, V>> blpop(long timeout, K... keys) {
        return createSingle(() -> commandBuilder.blpop(timeout, keys));
    }

    @Override
    public Single<KeyValue<K, V>> brpop(long timeout, K... keys) {
        return createSingle(() -> commandBuilder.brpop(timeout, keys));
    }

    @Override
    public Single<V> brpoplpush(long timeout, K source, K destination) {
        return createSingle(() -> commandBuilder.brpoplpush(timeout, source, destination));
    }

    @Override
    public Single<K> clientGetname() {
        return createSingle(commandBuilder::clientGetname);
    }

    @Override
    public Single<String> clientSetname(K name) {
        return createSingle(() -> commandBuilder.clientSetname(name));
    }

    @Override
    public Single<String> clientKill(String addr) {
        return createSingle(() -> commandBuilder.clientKill(addr));
    }

    @Override
    public Single<Long> clientKill(KillArgs killArgs) {
        return createSingle(() -> commandBuilder.clientKill(killArgs));
    }

    @Override
    public Single<String> clientPause(long timeout) {
        return createSingle(() -> commandBuilder.clientPause(timeout));
    }

    @Override
    public Single<String> clientList() {
        return createSingle(commandBuilder::clientList);
    }

    @Override
    public Observable<Object> command() {
        return createDissolvingObservable(commandBuilder::command);
    }

    @Override
    public Observable<Object> commandInfo(String... commands) {
        return createDissolvingObservable(() -> commandBuilder.commandInfo(commands));
    }

    @Override
    public Observable<Object> commandInfo(CommandType... commands) {
        String[] stringCommands = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            stringCommands[i] = commands[i].name();
        }

        return commandInfo(stringCommands);
    }

    @Override
    public Single<Long> commandCount() {
        return createSingle(commandBuilder::commandCount);
    }

    @Override
    public Observable<String> configGet(String parameter) {
        return createDissolvingObservable(() -> commandBuilder.configGet(parameter));
    }

    @Override
    public Single<String> configResetstat() {
        return createSingle(commandBuilder::configResetstat);
    }

    @Override
    public Single<String> configSet(String parameter, String value) {
        return createSingle(() -> commandBuilder.configSet(parameter, value));
    }

    @Override
    public Single<String> configRewrite() {
        return createSingle(commandBuilder::configRewrite);
    }

    @Override
    public Single<Long> dbsize() {
        return createSingle(commandBuilder::dbsize);
    }

    @Override
    public Single<String> debugCrashAndRecover(Long delay) {
        return createSingle(() -> (commandBuilder.debugCrashAndRecover(delay)));
    }

    @Override
    public Single<String> debugHtstats(int db) {
        return createSingle(() -> commandBuilder.debugHtstats(db));
    }

    @Override
    public Single<String> debugObject(K key) {
        return createSingle(() -> commandBuilder.debugObject(key));
    }

    @Override
    public Completable debugOom() {
        return Completable.fromObservable(createObservable(commandBuilder::debugOom));
    }

    @Override
    public Single<String> debugReload() {
        return createSingle(() -> (commandBuilder.debugReload()));
    }

    @Override
    public Single<String> debugRestart(Long delay) {
        return createSingle(() -> (commandBuilder.debugRestart(delay)));
    }

    @Override
    public Single<String> debugSdslen(K key) {
        return createSingle(() -> (commandBuilder.debugSdslen(key)));
    }

    @Override
    public Completable debugSegfault() {
        return Completable.fromObservable(createObservable(commandBuilder::debugSegfault));
    }

    @Override
    public Single<Long> decr(K key) {
        return createSingle(() -> commandBuilder.decr(key));
    }

    @Override
    public Single<Long> decrby(K key, long amount) {
        return createSingle(() -> commandBuilder.decrby(key, amount));
    }

    @Override
    public Single<Long> del(K... keys) {
        return createSingle(() -> commandBuilder.del(keys));
    }

    public Single<Long> del(Iterable<K> keys) {
        return createSingle(() -> commandBuilder.del(keys));
    }

    @Override
    public Single<Long> unlink(K... keys) {
        return createSingle(() -> commandBuilder.unlink(keys));
    }

    public Single<Long> unlink(Iterable<K> keys) {
        return createSingle(() -> commandBuilder.unlink(keys));
    }

    @Override
    public Single<String> discard() {
        return createSingle(commandBuilder::discard);
    }

    @Override
    public Single<byte[]> dump(K key) {
        return createSingle(() -> commandBuilder.dump(key));
    }

    @Override
    public Single<V> echo(V msg) {
        return createSingle(() -> commandBuilder.echo(msg));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Observable<T> eval(String script, ScriptOutputType type, K... keys) {
        return (Observable<T>) createObservable(() -> commandBuilder.eval(script, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Observable<T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        return (Observable<T>) createObservable(() -> commandBuilder.eval(script, type, keys, values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Observable<T> evalsha(String digest, ScriptOutputType type, K... keys) {
        return (Observable<T>) createObservable(() -> commandBuilder.evalsha(digest, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Observable<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        return (Observable<T>) createObservable(() -> commandBuilder.evalsha(digest, type, keys, values));
    }

    public Single<Boolean> exists(K key) {
        return createSingle(() -> commandBuilder.exists(key));
    }

    @Override
    public Single<Long> exists(K... keys) {
        return createSingle(() -> commandBuilder.exists(keys));
    }

    public Single<Long> exists(Iterable<K> keys) {
        return createSingle(() -> commandBuilder.exists(keys));
    }

    @Override
    public Single<Boolean> expire(K key, long seconds) {
        return createSingle(() -> commandBuilder.expire(key, seconds));
    }

    @Override
    public Single<Boolean> expireat(K key, long timestamp) {
        return createSingle(() -> commandBuilder.expireat(key, timestamp));
    }

    @Override
    public Single<Boolean> expireat(K key, Date timestamp) {
        return expireat(key, timestamp.getTime() / 1000);
    }

    @Override
    public Observable<Object> exec() {
        return createDissolvingObservable(EXEC, null, null);
    }

    @Override
    public Single<String> flushall() {
        return createSingle(commandBuilder::flushall);
    }

    @Override
    public Single<String> flushallAsync() {
        return createSingle(commandBuilder::flushallAsync);
    }

    @Override
    public Single<String> flushdb() {
        return createSingle(commandBuilder::flushdb);
    }

    @Override
    public Single<String> flushdbAsync() {
        return createSingle(commandBuilder::flushdbAsync);
    }

    @Override
    public Single<V> get(K key) {
        return createSingle(() -> commandBuilder.get(key));
    }

    @Override
    public Single<Long> getbit(K key, long offset) {
        return createSingle(() -> commandBuilder.getbit(key, offset));
    }

    @Override
    public Single<V> getrange(K key, long start, long end) {
        return createSingle(() -> commandBuilder.getrange(key, start, end));
    }

    @Override
    public Single<V> getset(K key, V value) {
        return createSingle(() -> commandBuilder.getset(key, value));
    }

    @Override
    public Single<Long> hdel(K key, K... fields) {
        return createSingle(() -> commandBuilder.hdel(key, fields));
    }

    @Override
    public Single<Boolean> hexists(K key, K field) {
        return createSingle(() -> commandBuilder.hexists(key, field));
    }

    @Override
    public Single<V> hget(K key, K field) {
        return createSingle(() -> commandBuilder.hget(key, field));
    }

    @Override
    public Single<Long> hincrby(K key, K field, long amount) {
        return createSingle(() -> commandBuilder.hincrby(key, field, amount));
    }

    @Override
    public Single<Double> hincrbyfloat(K key, K field, double amount) {
        return createSingle(() -> commandBuilder.hincrbyfloat(key, field, amount));
    }

    @Override
    public Single<Map<K, V>> hgetall(K key) {
        return createSingle(() -> commandBuilder.hgetall(key));
    }

    @Override
    public Single<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        return createSingle(() -> commandBuilder.hgetall(channel, key));
    }

    @Override
    public Observable<K> hkeys(K key) {
        return createDissolvingObservable(() -> commandBuilder.hkeys(key));
    }

    @Override
    public Single<Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        return createSingle(() -> commandBuilder.hkeys(channel, key));
    }

    @Override
    public Single<Long> hlen(K key) {
        return createSingle(() -> commandBuilder.hlen(key));
    }

    @Override
    public Single<Long> hstrlen(K key, K field) {
        return createSingle(() -> commandBuilder.hstrlen(key, field));
    }

    @Override
    public Observable<KeyValue<K, V>> hmget(K key, K... fields) {
        return createDissolvingObservable(() -> commandBuilder.hmgetKeyValue(key, fields));
    }

    @Override
    public Single<Long> hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        return createSingle(() -> commandBuilder.hmget(channel, key, fields));
    }

    @Override
    public Single<String> hmset(K key, Map<K, V> map) {
        return createSingle(() -> commandBuilder.hmset(key, map));
    }

    @Override
    public Single<Boolean> hset(K key, K field, V value) {
        return createSingle(() -> commandBuilder.hset(key, field, value));
    }

    @Override
    public Single<Boolean> hsetnx(K key, K field, V value) {
        return createSingle(() -> commandBuilder.hsetnx(key, field, value));
    }

    @Override
    public Observable<V> hvals(K key) {
        return createDissolvingObservable(() -> commandBuilder.hvals(key));
    }

    @Override
    public Single<Long> hvals(ValueStreamingChannel<V> channel, K key) {
        return createSingle(() -> commandBuilder.hvals(channel, key));
    }

    @Override
    public Single<Long> incr(K key) {
        return createSingle(() -> commandBuilder.incr(key));
    }

    @Override
    public Single<Long> incrby(K key, long amount) {
        return createSingle(() -> commandBuilder.incrby(key, amount));
    }

    @Override
    public Single<Double> incrbyfloat(K key, double amount) {
        return createSingle(() -> commandBuilder.incrbyfloat(key, amount));
    }

    @Override
    public Single<String> info() {
        return createSingle(commandBuilder::info);
    }

    @Override
    public Single<String> info(String section) {
        return createSingle(() -> commandBuilder.info(section));
    }

    @Override
    public Observable<K> keys(K pattern) {
        return createDissolvingObservable(() -> commandBuilder.keys(pattern));
    }

    @Override
    public Single<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        return createSingle(() -> commandBuilder.keys(channel, pattern));
    }

    @Override
    public Single<Date> lastsave() {
        return createSingle(commandBuilder::lastsave);
    }

    @Override
    public Single<V> lindex(K key, long index) {
        return createSingle(() -> commandBuilder.lindex(key, index));
    }

    @Override
    public Single<Long> linsert(K key, boolean before, V pivot, V value) {
        return createSingle(() -> commandBuilder.linsert(key, before, pivot, value));
    }

    @Override
    public Single<Long> llen(K key) {
        return createSingle(() -> commandBuilder.llen(key));
    }

    @Override
    public Single<V> lpop(K key) {
        return createSingle(() -> commandBuilder.lpop(key));
    }

    @Override
    public Single<Long> lpush(K key, V... values) {
        return createSingle(() -> commandBuilder.lpush(key, values));
    }

    @Override
    public Single<Long> lpushx(K key, V... values) {
        return createSingle(() -> commandBuilder.lpushx(key, values));
    }

    @Override
    public Observable<V> lrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.lrange(key, start, stop));
    }

    @Override
    public Single<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createSingle(() -> commandBuilder.lrange(channel, key, start, stop));
    }

    @Override
    public Single<Long> lrem(K key, long count, V value) {
        return createSingle(() -> commandBuilder.lrem(key, count, value));
    }

    @Override
    public Single<String> lset(K key, long index, V value) {
        return createSingle(() -> commandBuilder.lset(key, index, value));
    }

    @Override
    public Single<String> ltrim(K key, long start, long stop) {
        return createSingle(() -> commandBuilder.ltrim(key, start, stop));
    }

    @Override
    public Single<String> migrate(String host, int port, K key, int db, long timeout) {
        return createSingle(() -> commandBuilder.migrate(host, port, key, db, timeout));
    }

    @Override
    public Single<String> migrate(String host, int port, int db, long timeout, MigrateArgs<K> migrateArgs) {
        return createSingle(() -> commandBuilder.migrate(host, port, db, timeout, migrateArgs));
    }

    @Override
    public Observable<KeyValue<K, V>> mget(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.mgetKeyValue(keys));
    }

    public Observable<KeyValue<K, V>> mget(Iterable<K> keys) {
        return createDissolvingObservable(() -> commandBuilder.mgetKeyValue(keys));
    }

    @Override
    public Single<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return createSingle(() -> commandBuilder.mget(channel, keys));
    }

    public Single<Long> mget(ValueStreamingChannel<V> channel, Iterable<K> keys) {
        return createSingle(() -> commandBuilder.mget(channel, keys));
    }

    public Single<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        return createSingle(() -> commandBuilder.mget(channel, keys));
    }

    @Override
    public Single<Boolean> move(K key, int db) {
        return createSingle(() -> commandBuilder.move(key, db));
    }

    @Override
    public Single<String> multi() {
        return createSingle(commandBuilder::multi);
    }

    @Override
    public Single<String> mset(Map<K, V> map) {
        return createSingle(() -> commandBuilder.mset(map));
    }

    @Override
    public Single<Boolean> msetnx(Map<K, V> map) {
        return createSingle(() -> commandBuilder.msetnx(map));
    }

    @Override
    public Single<String> objectEncoding(K key) {
        return createSingle(() -> commandBuilder.objectEncoding(key));
    }

    @Override
    public Single<Long> objectIdletime(K key) {
        return createSingle(() -> commandBuilder.objectIdletime(key));
    }

    @Override
    public Single<Long> objectRefcount(K key) {
        return createSingle(() -> commandBuilder.objectRefcount(key));
    }

    @Override
    public Single<Boolean> persist(K key) {
        return createSingle(() -> commandBuilder.persist(key));
    }

    @Override
    public Single<Boolean> pexpire(K key, long milliseconds) {
        return createSingle(() -> commandBuilder.pexpire(key, milliseconds));
    }

    @Override
    public Single<Boolean> pexpireat(K key, Date timestamp) {
        return pexpireat(key, timestamp.getTime());
    }

    @Override
    public Single<Boolean> pexpireat(K key, long timestamp) {
        return createSingle(() -> commandBuilder.pexpireat(key, timestamp));
    }

    @Override
    public Single<String> ping() {
        return createSingle(commandBuilder::ping);
    }

    @Override
    public Single<String> readOnly() {
        return createSingle(commandBuilder::readOnly);
    }

    @Override
    public Single<String> readWrite() {
        return createSingle(commandBuilder::readWrite);
    }

    @Override
    public Single<Long> pttl(K key) {
        return createSingle(() -> commandBuilder.pttl(key));
    }

    @Override
    public Single<Long> publish(K channel, V message) {
        return createSingle(() -> commandBuilder.publish(channel, message));
    }

    @Override
    public Observable<K> pubsubChannels() {
        return createDissolvingObservable(commandBuilder::pubsubChannels);
    }

    @Override
    public Observable<K> pubsubChannels(K channel) {
        return createDissolvingObservable(() -> commandBuilder.pubsubChannels(channel));
    }

    @Override
    public Single<Map<K, Long>> pubsubNumsub(K... channels) {
        return createSingle(() -> commandBuilder.pubsubNumsub(channels));
    }

    @Override
    public Single<Long> pubsubNumpat() {
        return createSingle(commandBuilder::pubsubNumpat);
    }

    @Override
    public Single<String> quit() {
        return createSingle(commandBuilder::quit);
    }

    @Override
    public Observable<Object> role() {
        return createDissolvingObservable(commandBuilder::role);
    }

    @Override
    public Single<V> randomkey() {
        return createSingle(commandBuilder::randomkey);
    }

    @Override
    public Single<String> rename(K key, K newKey) {
        return createSingle(() -> commandBuilder.rename(key, newKey));
    }

    @Override
    public Single<Boolean> renamenx(K key, K newKey) {
        return createSingle(() -> commandBuilder.renamenx(key, newKey));
    }

    @Override
    public Single<String> restore(K key, long ttl, byte[] value) {
        return createSingle(() -> commandBuilder.restore(key, ttl, value));
    }

    @Override
    public Single<V> rpop(K key) {
        return createSingle(() -> commandBuilder.rpop(key));
    }

    @Override
    public Single<V> rpoplpush(K source, K destination) {
        return createSingle(() -> commandBuilder.rpoplpush(source, destination));
    }

    @Override
    public Single<Long> rpush(K key, V... values) {
        return createSingle(() -> commandBuilder.rpush(key, values));
    }

    @Override
    public Single<Long> rpushx(K key, V... values) {
        return createSingle(() -> commandBuilder.rpushx(key, values));
    }

    @Override
    public Single<Long> sadd(K key, V... members) {
        return createSingle(() -> commandBuilder.sadd(key, members));
    }

    @Override
    public Single<String> save() {
        return createSingle(commandBuilder::save);
    }

    @Override
    public Single<Long> scard(K key) {
        return createSingle(() -> commandBuilder.scard(key));
    }

    @Override
    public Observable<Boolean> scriptExists(String... digests) {
        return createDissolvingObservable(() -> commandBuilder.scriptExists(digests));
    }

    @Override
    public Single<String> scriptFlush() {
        return createSingle(commandBuilder::scriptFlush);
    }

    @Override
    public Single<String> scriptKill() {
        return createSingle(commandBuilder::scriptKill);
    }

    @Override
    public Single<String> scriptLoad(V script) {
        return createSingle(() -> commandBuilder.scriptLoad(script));
    }

    @Override
    public Observable<V> sdiff(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sdiff(keys));
    }

    @Override
    public Single<Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        return createSingle(() -> commandBuilder.sdiff(channel, keys));
    }

    @Override
    public Single<Long> sdiffstore(K destination, K... keys) {
        return createSingle(() -> commandBuilder.sdiffstore(destination, keys));
    }

    public Single<String> select(int db) {
        return createSingle(() -> commandBuilder.select(db));
    }

    @Override
    public Single<String> set(K key, V value) {
        return createSingle(() -> commandBuilder.set(key, value));
    }

    @Override
    public Single<String> set(K key, V value, SetArgs setArgs) {
        return createSingle(() -> commandBuilder.set(key, value, setArgs));
    }

    @Override
    public Single<Long> setbit(K key, long offset, int value) {
        return createSingle(() -> commandBuilder.setbit(key, offset, value));
    }

    @Override
    public Single<String> setex(K key, long seconds, V value) {
        return createSingle(() -> commandBuilder.setex(key, seconds, value));
    }

    @Override
    public Single<String> psetex(K key, long milliseconds, V value) {
        return createSingle(() -> commandBuilder.psetex(key, milliseconds, value));
    }

    @Override
    public Single<Boolean> setnx(K key, V value) {
        return createSingle(() -> commandBuilder.setnx(key, value));
    }

    @Override
    public Single<Long> setrange(K key, long offset, V value) {
        return createSingle(() -> commandBuilder.setrange(key, offset, value));
    }

    @Override
    public Completable shutdown(boolean save) {
        return Completable.fromObservable(createObservable(() -> commandBuilder.shutdown(save)));
    }

    @Override
    public Observable<V> sinter(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sinter(keys));
    }

    @Override
    public Single<Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        return createSingle(() -> commandBuilder.sinter(channel, keys));
    }

    @Override
    public Single<Long> sinterstore(K destination, K... keys) {
        return createSingle(() -> commandBuilder.sinterstore(destination, keys));
    }

    @Override
    public Single<Boolean> sismember(K key, V member) {
        return createSingle(() -> commandBuilder.sismember(key, member));
    }

    @Override
    public Single<Boolean> smove(K source, K destination, V member) {
        return createSingle(() -> commandBuilder.smove(source, destination, member));
    }

    @Override
    public Single<String> slaveof(String host, int port) {
        return createSingle(() -> commandBuilder.slaveof(host, port));
    }

    @Override
    public Single<String> slaveofNoOne() {
        return createSingle(() -> commandBuilder.slaveofNoOne());
    }

    @Override
    public Observable<Object> slowlogGet() {
        return createDissolvingObservable(() -> commandBuilder.slowlogGet());
    }

    @Override
    public Observable<Object> slowlogGet(int count) {
        return createDissolvingObservable(() -> commandBuilder.slowlogGet(count));
    }

    @Override
    public Single<Long> slowlogLen() {
        return createSingle(() -> commandBuilder.slowlogLen());
    }

    @Override
    public Single<String> slowlogReset() {
        return createSingle(() -> commandBuilder.slowlogReset());
    }

    @Override
    public Observable<V> smembers(K key) {
        return createDissolvingObservable(() -> commandBuilder.smembers(key));
    }

    @Override
    public Single<Long> smembers(ValueStreamingChannel<V> channel, K key) {
        return createSingle(() -> commandBuilder.smembers(channel, key));
    }

    @Override
    public Observable<V> sort(K key) {
        return createDissolvingObservable(() -> commandBuilder.sort(key));
    }

    @Override
    public Single<Long> sort(ValueStreamingChannel<V> channel, K key) {
        return createSingle(() -> commandBuilder.sort(channel, key));
    }

    @Override
    public Observable<V> sort(K key, SortArgs sortArgs) {
        return createDissolvingObservable(() -> commandBuilder.sort(key, sortArgs));
    }

    @Override
    public Single<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        return createSingle(() -> commandBuilder.sort(channel, key, sortArgs));
    }

    @Override
    public Single<Long> sortStore(K key, SortArgs sortArgs, K destination) {
        return createSingle(() -> commandBuilder.sortStore(key, sortArgs, destination));
    }

    @Override
    public Single<V> spop(K key) {
        return createSingle(() -> commandBuilder.spop(key));
    }

    @Override
    public Observable<V> spop(K key, long count) {
        return createDissolvingObservable(() -> commandBuilder.spop(key, count));
    }

    @Override
    public Single<V> srandmember(K key) {
        return createSingle(() -> commandBuilder.srandmember(key));
    }

    @Override
    public Observable<V> srandmember(K key, long count) {
        return createDissolvingObservable(() -> commandBuilder.srandmember(key, count));
    }

    @Override
    public Single<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        return createSingle(() -> commandBuilder.srandmember(channel, key, count));
    }

    @Override
    public Single<Long> srem(K key, V... members) {
        return createSingle(() -> commandBuilder.srem(key, members));
    }

    @Override
    public Observable<V> sunion(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sunion(keys));
    }

    @Override
    public Single<Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        return createSingle(() -> commandBuilder.sunion(channel, keys));
    }

    @Override
    public Single<Long> sunionstore(K destination, K... keys) {
        return createSingle(() -> commandBuilder.sunionstore(destination, keys));
    }

    @Override
    public Single<Long> strlen(K key) {
        return createSingle(() -> commandBuilder.strlen(key));
    }

    @Override
    public Single<Long> touch(K... keys) {
        return createSingle(() -> commandBuilder.touch(keys));
    }

    public Single<Long> touch(Iterable<K> keys) {
        return createSingle(() -> commandBuilder.touch(keys));
    }

    @Override
    public Single<Long> ttl(K key) {
        return createSingle(() -> commandBuilder.ttl(key));
    }

    @Override
    public Single<String> type(K key) {
        return createSingle(() -> commandBuilder.type(key));
    }

    @Override
    public Single<String> watch(K... keys) {
        return createSingle(() -> commandBuilder.watch(keys));
    }

    @Override
    public Single<String> unwatch() {
        return createSingle(commandBuilder::unwatch);
    }

    @Override
    public Single<Long> zadd(K key, double score, V member) {
        return createSingle(() -> commandBuilder.zadd(key, null, score, member));
    }

    @Override
    public Single<Long> zadd(K key, Object... scoresAndValues) {
        return createSingle(() -> commandBuilder.zadd(key, null, scoresAndValues));
    }

    @Override
    public Single<Long> zadd(K key, ScoredValue<V>... scoredValues) {
        return createSingle(() -> commandBuilder.zadd(key, null, (Object[]) scoredValues));
    }

    @Override
    public Single<Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return createSingle(() -> commandBuilder.zadd(key, zAddArgs, score, member));
    }

    @Override
    public Single<Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        return createSingle(() -> commandBuilder.zadd(key, zAddArgs, scoresAndValues));
    }

    @Override
    public Single<Long> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... scoredValues) {
        return createSingle(() -> commandBuilder.zadd(key, zAddArgs, (Object[]) scoredValues));
    }

    @Override
    public Single<Double> zaddincr(K key, double score, V member) {
        return createSingle(() -> commandBuilder.zaddincr(key, score, member));
    }

    @Override
    public Single<Long> zcard(K key) {
        return createSingle(() -> commandBuilder.zcard(key));
    }

    @Override
    public Single<Long> zcount(K key, double min, double max) {
        return createSingle(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Single<Long> zcount(K key, String min, String max) {
        return createSingle(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Single<Double> zincrby(K key, double amount, K member) {
        return createSingle(() -> commandBuilder.zincrby(key, amount, member));
    }

    @Override
    public Single<Long> zinterstore(K destination, K... keys) {
        return createSingle(() -> commandBuilder.zinterstore(destination, keys));
    }

    @Override
    public Single<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createSingle(() -> commandBuilder.zinterstore(destination, storeArgs, keys));
    }

    @Override
    public Observable<V> zrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrange(key, start, stop));
    }

    @Override
    public Observable<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrangeWithScores(key, start, stop));
    }

    @Override
    public Observable<V> zrangebyscore(K key, double min, double max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public Observable<V> zrangebyscore(K key, String min, String max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public Observable<V> zrangebyscore(K key, double min, double max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public Observable<V> zrangebyscore(K key, String min, String max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public Single<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createSingle(() -> commandBuilder.zrange(channel, key, start, stop));
    }

    @Override
    public Single<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createSingle(() -> commandBuilder.zrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Single<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return createSingle(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Single<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        return createSingle(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Single<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset,
            long count) {
        return createSingle(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Single<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset,
            long count) {
        return createSingle(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Single<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return createSingle(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Single<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        return createSingle(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Single<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return createSingle(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Single<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        return createSingle(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Single<Long> zrank(K key, V member) {
        return createSingle(() -> commandBuilder.zrank(key, member));
    }

    @Override
    public Single<Long> zrem(K key, V... members) {
        return createSingle(() -> commandBuilder.zrem(key, members));
    }

    @Override
    public Single<Long> zremrangebyrank(K key, long start, long stop) {
        return createSingle(() -> commandBuilder.zremrangebyrank(key, start, stop));
    }

    @Override
    public Single<Long> zremrangebyscore(K key, double min, double max) {
        return createSingle(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Single<Long> zremrangebyscore(K key, String min, String max) {
        return createSingle(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Observable<V> zrevrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrevrange(key, start, stop));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangeWithScores(key, start, stop));
    }

    @Override
    public Observable<V> zrevrangebyscore(K key, double max, double min) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public Observable<V> zrevrangebyscore(K key, String max, String min) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public Observable<V> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public Observable<V> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public Single<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createSingle(() -> commandBuilder.zrevrange(channel, key, start, stop));
    }

    @Override
    public Single<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createSingle(() -> commandBuilder.zrevrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Single<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return createSingle(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Single<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        return createSingle(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Single<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return createSingle(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Single<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        return createSingle(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Single<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return createSingle(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Single<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        return createSingle(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Single<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count) {
        return createSingle(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Single<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count) {
        return createSingle(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Single<Long> zrevrank(K key, V member) {
        return createSingle(() -> commandBuilder.zrevrank(key, member));
    }

    @Override
    public Single<Double> zscore(K key, V member) {
        return createSingle(() -> commandBuilder.zscore(key, member));
    }

    @Override
    public Single<Long> zunionstore(K destination, K... keys) {
        return createSingle(() -> commandBuilder.zunionstore(destination, keys));
    }

    @Override
    public Single<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createSingle(() -> commandBuilder.zunionstore(destination, storeArgs, keys));
    }

    @Override
    public Single<KeyScanCursor<K>> scan() {
        return createSingle(commandBuilder::scan);
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.scan(scanArgs));
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.scan(scanCursor, scanArgs));
    }

    @Override
    public Single<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.scan(scanCursor));
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return createSingle(() -> commandBuilder.scanStreaming(channel));
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.scanStreaming(channel, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.scanStreaming(channel, scanCursor, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.scanStreaming(channel, scanCursor));
    }

    @Override
    public Single<ValueScanCursor<V>> sscan(K key) {
        return createSingle(() -> commandBuilder.sscan(key));
    }

    @Override
    public Single<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.sscan(key, scanArgs));
    }

    @Override
    public Single<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.sscan(key, scanCursor, scanArgs));
    }

    @Override
    public Single<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.sscan(key, scanCursor));
    }

    @Override
    public Single<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return createSingle(() -> commandBuilder.sscanStreaming(channel, key));
    }

    @Override
    public Single<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.sscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.sscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.sscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Single<MapScanCursor<K, V>> hscan(K key) {
        return createSingle(() -> commandBuilder.hscan(key));
    }

    @Override
    public Single<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.hscan(key, scanArgs));
    }

    @Override
    public Single<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.hscan(key, scanCursor, scanArgs));
    }

    @Override
    public Single<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.hscan(key, scanCursor));
    }

    @Override
    public Single<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return createSingle(() -> commandBuilder.hscanStreaming(channel, key));
    }

    @Override
    public Single<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.hscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.hscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.hscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Single<ScoredValueScanCursor<V>> zscan(K key) {
        return createSingle(() -> commandBuilder.zscan(key));
    }

    @Override
    public Single<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.zscan(key, scanArgs));
    }

    @Override
    public Single<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.zscan(key, scanCursor, scanArgs));
    }

    @Override
    public Single<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.zscan(key, scanCursor));
    }

    @Override
    public Single<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return createSingle(() -> commandBuilder.zscanStreaming(channel, key));
    }

    @Override
    public Single<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.zscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createSingle(() -> commandBuilder.zscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Single<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createSingle(() -> commandBuilder.zscanStreaming(channel, key, scanCursor));
    }

    @Override
    public String digest(V script) {
        return LettuceStrings.digest(codec.encodeValue(script));
    }

    @Override
    public Observable<V> time() {
        return createDissolvingObservable(commandBuilder::time);
    }

    @Override
    public Single<Long> waitForReplication(int replicas, long timeout) {
        return createSingle(() -> commandBuilder.wait(replicas, timeout));
    }

    @Override
    public Single<Long> pfadd(K key, V... values) {
        return createSingle(() -> commandBuilder.pfadd(key, values));
    }

    public Single<Long> pfadd(K key, V value, V... values) {
        return createSingle(() -> commandBuilder.pfadd(key, value, values));
    }

    @Override
    public Single<String> pfmerge(K destkey, K... sourcekeys) {
        return createSingle(() -> commandBuilder.pfmerge(destkey, sourcekeys));
    }

    public Single<String> pfmerge(K destkey, K sourceKey, K... sourcekeys) {
        return createSingle(() -> commandBuilder.pfmerge(destkey, sourceKey, sourcekeys));
    }

    @Override
    public Single<Long> pfcount(K... keys) {
        return createSingle(() -> commandBuilder.pfcount(keys));
    }

    public Single<Long> pfcount(K key, K... keys) {
        return createSingle(() -> commandBuilder.pfcount(key, keys));
    }

    @Override
    public Single<String> clusterBumpepoch() {
        return createSingle(() -> commandBuilder.clusterBumpepoch());
    }

    @Override
    public Single<String> clusterMeet(String ip, int port) {
        return createSingle(() -> commandBuilder.clusterMeet(ip, port));
    }

    @Override
    public Single<String> clusterForget(String nodeId) {
        return createSingle(() -> commandBuilder.clusterForget(nodeId));
    }

    @Override
    public Single<String> clusterAddSlots(int... slots) {
        return createSingle(() -> commandBuilder.clusterAddslots(slots));
    }

    @Override
    public Single<String> clusterDelSlots(int... slots) {
        return createSingle(() -> commandBuilder.clusterDelslots(slots));
    }

    @Override
    public Single<String> clusterInfo() {
        return createSingle(commandBuilder::clusterInfo);
    }

    @Override
    public Single<String> clusterMyId() {
        return createSingle(commandBuilder::clusterMyId);
    }

    @Override
    public Single<String> clusterNodes() {
        return createSingle(commandBuilder::clusterNodes);
    }

    @Override
    public Observable<K> clusterGetKeysInSlot(int slot, int count) {
        return createDissolvingObservable(() -> commandBuilder.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public Single<Long> clusterCountKeysInSlot(int slot) {
        return createSingle(() -> commandBuilder.clusterCountKeysInSlot(slot));
    }

    @Override
    public Single<Long> clusterCountFailureReports(String nodeId) {
        return createSingle(() -> commandBuilder.clusterCountFailureReports(nodeId));
    }

    @Override
    public Single<Long> clusterKeyslot(K key) {
        return createSingle(() -> commandBuilder.clusterKeyslot(key));
    }

    @Override
    public Single<String> clusterSaveconfig() {
        return createSingle(() -> commandBuilder.clusterSaveconfig());
    }

    @Override
    public Single<String> clusterSetConfigEpoch(long configEpoch) {
        return createSingle(() -> commandBuilder.clusterSetConfigEpoch(configEpoch));
    }

    @Override
    public Observable<Object> clusterSlots() {
        return createDissolvingObservable(commandBuilder::clusterSlots);
    }

    @Override
    public Single<String> clusterSetSlotNode(int slot, String nodeId) {
        return createSingle(() -> commandBuilder.clusterSetSlotNode(slot, nodeId));
    }

    @Override
    public Single<String> clusterSetSlotStable(int slot) {
        return createSingle(() -> commandBuilder.clusterSetSlotStable(slot));
    }

    @Override
    public Single<String> clusterSetSlotMigrating(int slot, String nodeId) {
        return createSingle(() -> commandBuilder.clusterSetSlotMigrating(slot, nodeId));
    }

    @Override
    public Single<String> clusterSetSlotImporting(int slot, String nodeId) {
        return createSingle(() -> commandBuilder.clusterSetSlotImporting(slot, nodeId));
    }

    @Override
    public Single<String> clusterFailover(boolean force) {
        return createSingle(() -> commandBuilder.clusterFailover(force));
    }

    @Override
    public Single<String> clusterReset(boolean hard) {
        return createSingle(() -> commandBuilder.clusterReset(hard));
    }

    @Override
    public Single<String> asking() {
        return createSingle(commandBuilder::asking);
    }

    @Override
    public Single<String> clusterReplicate(String nodeId) {
        return createSingle(() -> commandBuilder.clusterReplicate(nodeId));
    }

    @Override
    public Single<String> clusterFlushslots() {
        return createSingle(commandBuilder::clusterFlushslots);
    }

    @Override
    public Observable<String> clusterSlaves(String nodeId) {
        return createDissolvingObservable(() -> commandBuilder.clusterSlaves(nodeId));
    }

    @Override
    public Single<Long> zlexcount(K key, String min, String max) {
        return createSingle(() -> commandBuilder.zlexcount(key, min, max));
    }

    @Override
    public Single<Long> zremrangebylex(K key, String min, String max) {
        return createSingle(() -> commandBuilder.zremrangebylex(key, min, max));
    }

    @Override
    public Observable<V> zrangebylex(K key, String min, String max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, min, max));
    }

    @Override
    public Observable<V> zrangebylex(K key, String min, String max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, min, max, offset, count));
    }

    @Override
    public Single<Long> geoadd(K key, double longitude, double latitude, V member) {
        return createSingle(() -> commandBuilder.geoadd(key, longitude, latitude, member));
    }

    @Override
    public Single<Long> geoadd(K key, Object... lngLatMember) {
        return createSingle(() -> commandBuilder.geoadd(key, lngLatMember));
    }

    @Override
    public Observable<String> geohash(K key, V... members) {
        return createDissolvingObservable(() -> commandBuilder.geohash(key, members));
    }

    @Override
    public Observable<V> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadius(key, longitude, latitude, distance, unit.name()));
    }

    @Override
    public Observable<GeoWithin<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadius(key, longitude, latitude, distance, unit.name(),
                geoArgs));
    }

    @Override
    public Single<Long> georadius(K key, double longitude, double latitude, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createSingle(
                () -> commandBuilder.georadius(key, longitude, latitude, distance, unit.name(), geoRadiusStoreArgs));
    }

    @Override
    public Observable<V> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(key, member, distance, unit.name()));
    }

    @Override
    public Observable<GeoWithin<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(key, member, distance, unit.name(), geoArgs));
    }

    @Override
    public Single<Long> georadiusbymember(K key, V member, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createSingle(
                () -> commandBuilder.georadiusbymember(key, member, distance, unit.name(), geoRadiusStoreArgs));
    }

    @Override
    public Observable<GeoCoordinates> geopos(K key, V... members) {
        return createDissolvingObservable(() -> commandBuilder.geopos(key, members));
    }

    @Override
    public Single<Double> geodist(K key, V from, V to, GeoArgs.Unit unit) {
        return createSingle(() -> commandBuilder.geodist(key, from, to, unit));
    }

    public <T> Observable<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return createDissolvingObservable(() -> new Command<>(type, output));
    }

    public <T> Observable<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return createDissolvingObservable(() -> new Command<>(type, output, args));
    }

    protected <T> Observable<T> createObservable(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return createObservable(() -> new Command<>(type, output, args));
    }

    public <T> Observable<T> createObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return Observable.create(new ReactiveCommandDispatcher<K, V, T>(commandSupplier, connection, false).getObservableSubscriber());
    }

    protected <T> Single<T> createSingle(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return createSingle(() -> new Command<>(type, output, args));
    }

    public <T> Single<T> createSingle(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return Single.create(new ReactiveCommandDispatcher<K, V, T>(commandSupplier, connection, false).getSingleSubscriber());
    }
    
    @SuppressWarnings("unchecked")
    public <T, R> Observable<R> createDissolvingObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return (Observable<R>) Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, true).getObservableSubscriber());
    }

    @SuppressWarnings("unchecked")
    public <T, R> R createDissolvingObservable(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return (R) Observable.create(new ReactiveCommandDispatcher<K, V, T>(() -> new Command<>(type, output, args),
                connection, true).getObservableSubscriber());
    }

    /**
     * Emits just {@link Success#Success} or the {@link Throwable} after the inner observable is completed.
     *
     * @param observable inner observable
     * @param <T> used for type inference
     * @return Success observable
     */
    protected <T> Observable<Success> getSuccessObservable(final Observable<T> observable) {
        return Observable.create(new Observable.OnSubscribe<Success>() {
            @Override
            public void call(Subscriber<? super Success> subscriber) {

                observable.subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        subscriber.onNext(Success.Success);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onNext(Object k) {

                    }
                });
            }
        });
    }

    public void setTimeout(long timeout, TimeUnit unit) {
        connection.setTimeout(timeout, unit);
    }

    public void close() {
        connection.close();
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public void reset() {
        getConnection().reset();
    }

    public StatefulConnection<K, V> getConnection() {
        return connection;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        connection.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        connection.flushCommands();
    }
}
