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
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandType.*;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.async.*;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.*;

/**
 * An asynchronous and thread-safe API for a Redis connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRedisAsyncCommands<K, V> implements RedisHashAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>,
        RedisStringAsyncCommands<K, V>, RedisListAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>,
        RedisHLLAsyncCommands<K, V>, BaseRedisAsyncCommands<K, V>, RedisTransactionalAsyncCommands<K, V>,
        RedisGeoAsyncCommands<K, V>, RedisClusterAsyncCommands<K, V> {

    private final StatefulConnection<K, V> connection;

    private final RedisCodec<K, V> codec;

    private final RedisCommandBuilder<K, V> commandBuilder;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     */
    public AbstractRedisAsyncCommands(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        this.codec = codec;
        this.commandBuilder = new RedisCommandBuilder<>(codec);
    }

    @Override
    public RedisFuture<Long> append(K key, V value) {
        return dispatch(commandBuilder.append(key, value));
    }

    @Override
    public RedisFuture<String> asking() {
        return dispatch(commandBuilder.asking());
    }

    @Override
    public String auth(String password) {

        LettuceAssert.notNull(password, "Password must not be null");
        AsyncCommand<K, V, String> cmd = authAsync(password.toCharArray());
        return LettuceFutures.awaitOrCancel(cmd, connection.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    public AsyncCommand<K, V, String> authAsync(char[] password) {
        return dispatch(commandBuilder.auth(password));
    }

    @Override
    public RedisFuture<String> bgrewriteaof() {
        return dispatch(commandBuilder.bgrewriteaof());
    }

    @Override
    public RedisFuture<String> bgsave() {
        return dispatch(commandBuilder.bgsave());
    }

    @Override
    public RedisFuture<Long> bitcount(K key) {
        return dispatch(commandBuilder.bitcount(key));
    }

    @Override
    public RedisFuture<Long> bitcount(K key, long start, long end) {
        return dispatch(commandBuilder.bitcount(key, start, end));
    }

    @Override
    public RedisFuture<List<Long>> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return dispatch(commandBuilder.bitfield(key, bitFieldArgs));
    }

    @Override
    public RedisFuture<Long> bitopAnd(K destination, K... keys) {
        return dispatch(commandBuilder.bitopAnd(destination, keys));
    }

    @Override
    public RedisFuture<Long> bitopNot(K destination, K source) {
        return dispatch(commandBuilder.bitopNot(destination, source));
    }

    @Override
    public RedisFuture<Long> bitopOr(K destination, K... keys) {
        return dispatch(commandBuilder.bitopOr(destination, keys));
    }

    @Override
    public RedisFuture<Long> bitopXor(K destination, K... keys) {
        return dispatch(commandBuilder.bitopXor(destination, keys));
    }

    @Override
    public RedisFuture<Long> bitpos(K key, boolean state) {
        return dispatch(commandBuilder.bitpos(key, state));
    }

    @Override
    public RedisFuture<Long> bitpos(K key, boolean state, long start) {
        return dispatch(commandBuilder.bitpos(key, state, start));
    }

    @Override
    public RedisFuture<Long> bitpos(K key, boolean state, long start, long end) {
        return dispatch(commandBuilder.bitpos(key, state, start, end));
    }

    @Override
    public RedisFuture<KeyValue<K, V>> blpop(long timeout, K... keys) {
        return dispatch(commandBuilder.blpop(timeout, keys));
    }

    @Override
    public RedisFuture<KeyValue<K, V>> brpop(long timeout, K... keys) {
        return dispatch(commandBuilder.brpop(timeout, keys));
    }

    @Override
    public RedisFuture<V> brpoplpush(long timeout, K source, K destination) {
        return dispatch(commandBuilder.brpoplpush(timeout, source, destination));
    }

    @Override
    public RedisFuture<K> clientGetname() {
        return dispatch(commandBuilder.clientGetname());
    }

    @Override
    public RedisFuture<String> clientKill(String addr) {
        return dispatch(commandBuilder.clientKill(addr));
    }

    @Override
    public RedisFuture<Long> clientKill(KillArgs killArgs) {
        return dispatch(commandBuilder.clientKill(killArgs));
    }

    @Override
    public RedisFuture<String> clientList() {
        return dispatch(commandBuilder.clientList());
    }

    @Override
    public RedisFuture<Long> clientId() {
        return dispatch(commandBuilder.clientId());
    }

    @Override
    public RedisFuture<String> clientPause(long timeout) {
        return dispatch(commandBuilder.clientPause(timeout));
    }

    @Override
    public RedisFuture<String> clientSetname(K name) {
        return dispatch(commandBuilder.clientSetname(name));
    }

    @Override
    public RedisFuture<Long> clientUnblock(long id, UnblockType type) {
        return dispatch(commandBuilder.clientUnblock(id, type));
    }

    @Override
    public RedisFuture<String> clusterAddSlots(int... slots) {
        return dispatch(commandBuilder.clusterAddslots(slots));
    }

    @Override
    public RedisFuture<String> clusterBumpepoch() {
        return dispatch(commandBuilder.clusterBumpepoch());
    }

    @Override
    public RedisFuture<Long> clusterCountFailureReports(String nodeId) {
        return dispatch(commandBuilder.clusterCountFailureReports(nodeId));
    }

    @Override
    public RedisFuture<Long> clusterCountKeysInSlot(int slot) {
        return dispatch(commandBuilder.clusterCountKeysInSlot(slot));
    }

    @Override
    public RedisFuture<String> clusterDelSlots(int... slots) {
        return dispatch(commandBuilder.clusterDelslots(slots));
    }

    @Override
    public RedisFuture<String> clusterFailover(boolean force) {
        return dispatch(commandBuilder.clusterFailover(force));
    }

    @Override
    public RedisFuture<String> clusterFlushslots() {
        return dispatch(commandBuilder.clusterFlushslots());
    }

    @Override
    public RedisFuture<String> clusterForget(String nodeId) {
        return dispatch(commandBuilder.clusterForget(nodeId));
    }

    @Override
    public RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count) {
        return dispatch(commandBuilder.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public RedisFuture<String> clusterInfo() {
        return dispatch(commandBuilder.clusterInfo());
    }

    @Override
    public RedisFuture<Long> clusterKeyslot(K key) {
        return dispatch(commandBuilder.clusterKeyslot(key));
    }

    @Override
    public RedisFuture<String> clusterMeet(String ip, int port) {
        return dispatch(commandBuilder.clusterMeet(ip, port));
    }

    @Override
    public RedisFuture<String> clusterMyId() {
        return dispatch(commandBuilder.clusterMyId());
    }

    @Override
    public RedisFuture<String> clusterNodes() {
        return dispatch(commandBuilder.clusterNodes());
    }

    @Override
    public RedisFuture<String> clusterReplicate(String nodeId) {
        return dispatch(commandBuilder.clusterReplicate(nodeId));
    }

    @Override
    public RedisFuture<String> clusterReset(boolean hard) {
        return dispatch(commandBuilder.clusterReset(hard));
    }

    @Override
    public RedisFuture<String> clusterSaveconfig() {
        return dispatch(commandBuilder.clusterSaveconfig());
    }

    @Override
    public RedisFuture<String> clusterSetConfigEpoch(long configEpoch) {
        return dispatch(commandBuilder.clusterSetConfigEpoch(configEpoch));
    }

    @Override
    public RedisFuture<String> clusterSetSlotImporting(int slot, String nodeId) {
        return dispatch(commandBuilder.clusterSetSlotImporting(slot, nodeId));
    }

    @Override
    public RedisFuture<String> clusterSetSlotMigrating(int slot, String nodeId) {
        return dispatch(commandBuilder.clusterSetSlotMigrating(slot, nodeId));
    }

    @Override
    public RedisFuture<String> clusterSetSlotNode(int slot, String nodeId) {
        return dispatch(commandBuilder.clusterSetSlotNode(slot, nodeId));
    }

    @Override
    public RedisFuture<String> clusterSetSlotStable(int slot) {
        return dispatch(commandBuilder.clusterSetSlotStable(slot));
    }

    @Override
    public RedisFuture<List<String>> clusterSlaves(String nodeId) {
        return dispatch(commandBuilder.clusterSlaves(nodeId));
    }

    @Override
    public RedisFuture<List<Object>> clusterSlots() {
        return dispatch(commandBuilder.clusterSlots());
    }

    @Override
    public RedisFuture<List<Object>> command() {
        return dispatch(commandBuilder.command());
    }

    @Override
    public RedisFuture<Long> commandCount() {
        return dispatch(commandBuilder.commandCount());
    }

    @Override
    public RedisFuture<List<Object>> commandInfo(String... commands) {
        return dispatch(commandBuilder.commandInfo(commands));
    }

    @Override
    public RedisFuture<List<Object>> commandInfo(CommandType... commands) {
        String[] stringCommands = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            stringCommands[i] = commands[i].name();
        }

        return commandInfo(stringCommands);
    }

    @Override
    public RedisFuture<Map<String, String>> configGet(String parameter) {
        return dispatch(commandBuilder.configGet(parameter));
    }

    @Override
    public RedisFuture<String> configResetstat() {
        return dispatch(commandBuilder.configResetstat());
    }

    @Override
    public RedisFuture<String> configRewrite() {
        return dispatch(commandBuilder.configRewrite());
    }

    @Override
    public RedisFuture<String> configSet(String parameter, String value) {
        return dispatch(commandBuilder.configSet(parameter, value));
    }

    @Override
    public RedisFuture<Long> dbsize() {
        return dispatch(commandBuilder.dbsize());
    }

    @Override
    public RedisFuture<String> debugCrashAndRecover(Long delay) {
        return dispatch(commandBuilder.debugCrashAndRecover(delay));
    }

    @Override
    public RedisFuture<String> debugHtstats(int db) {
        return dispatch(commandBuilder.debugHtstats(db));
    }

    @Override
    public RedisFuture<String> debugObject(K key) {
        return dispatch(commandBuilder.debugObject(key));
    }

    @Override
    public void debugOom() {
        dispatch(commandBuilder.debugOom());
    }

    @Override
    public RedisFuture<String> debugReload() {
        return dispatch(commandBuilder.debugReload());
    }

    @Override
    public RedisFuture<String> debugRestart(Long delay) {
        return dispatch(commandBuilder.debugRestart(delay));
    }

    @Override
    public RedisFuture<String> debugSdslen(K key) {
        return dispatch(commandBuilder.debugSdslen(key));
    }

    @Override
    public void debugSegfault() {
        dispatch(commandBuilder.debugSegfault());
    }

    @Override
    public RedisFuture<Long> decr(K key) {
        return dispatch(commandBuilder.decr(key));
    }

    @Override
    public RedisFuture<Long> decrby(K key, long amount) {
        return dispatch(commandBuilder.decrby(key, amount));
    }

    @Override
    public RedisFuture<Long> del(K... keys) {
        return dispatch(commandBuilder.del(keys));
    }

    public RedisFuture<Long> del(Iterable<K> keys) {
        return dispatch(commandBuilder.del(keys));
    }

    @Override
    public String digest(V script) {
        return LettuceStrings.digest(codec.encodeValue(script));
    }

    @Override
    public RedisFuture<String> discard() {
        return dispatch(commandBuilder.discard());
    }

    @Override
    public <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return dispatch(new AsyncCommand<>(new Command<>(type, output)));
    }

    @Override
    public <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return dispatch(new AsyncCommand<>(new Command<>(type, output, args)));
    }

    protected <T> RedisFuture<T> dispatch(CommandType type, CommandOutput<K, V, T> output) {
        return dispatch(type, output, null);
    }

    protected <T> RedisFuture<T> dispatch(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return dispatch(new AsyncCommand<>(new Command<>(type, output, args)));
    }

    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        AsyncCommand<K, V, T> asyncCommand = new AsyncCommand<>(cmd);
        RedisCommand<K, V, T> dispatched = connection.dispatch(asyncCommand);
        if (dispatched instanceof AsyncCommand) {
            return (AsyncCommand<K, V, T>) dispatched;
        }
        return asyncCommand;
    }

    @Override
    public RedisFuture<byte[]> dump(K key) {
        return dispatch(commandBuilder.dump(key));
    }

    @Override
    public RedisFuture<V> echo(V msg) {
        return dispatch(commandBuilder.echo(msg));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RedisFuture<T> eval(String script, ScriptOutputType type, K... keys) {
        return (RedisFuture<T>) dispatch(commandBuilder.eval(script, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RedisFuture<T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        return (RedisFuture<T>) dispatch(commandBuilder.eval(script, type, keys, values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K... keys) {
        return (RedisFuture<T>) dispatch(commandBuilder.evalsha(digest, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        return (RedisFuture<T>) dispatch(commandBuilder.evalsha(digest, type, keys, values));
    }

    @Override
    public RedisFuture<TransactionResult> exec() {
        return dispatch(EXEC, null);
    }

    @Override
    public RedisFuture<Long> exists(K... keys) {
        return dispatch(commandBuilder.exists(keys));
    }

    public RedisFuture<Long> exists(Iterable<K> keys) {
        return dispatch(commandBuilder.exists(keys));
    }

    @Override
    public RedisFuture<Boolean> expire(K key, long seconds) {
        return dispatch(commandBuilder.expire(key, seconds));
    }

    @Override
    public RedisFuture<Boolean> expireat(K key, Date timestamp) {
        return expireat(key, timestamp.getTime() / 1000);
    }

    @Override
    public RedisFuture<Boolean> expireat(K key, long timestamp) {
        return dispatch(commandBuilder.expireat(key, timestamp));
    }

    @Override
    public void flushCommands() {
        connection.flushCommands();
    }

    @Override
    public RedisFuture<String> flushall() {
        return dispatch(commandBuilder.flushall());
    }

    @Override
    public RedisFuture<String> flushallAsync() {
        return dispatch(commandBuilder.flushallAsync());
    }

    @Override
    public RedisFuture<String> flushdb() {
        return dispatch(commandBuilder.flushdb());
    }

    @Override
    public RedisFuture<String> flushdbAsync() {
        return dispatch(commandBuilder.flushdbAsync());
    }

    @Override
    public RedisFuture<Long> geoadd(K key, double longitude, double latitude, V member) {
        return dispatch(commandBuilder.geoadd(key, longitude, latitude, member));
    }

    @Override
    public RedisFuture<Long> geoadd(K key, Object... lngLatMember) {
        return dispatch(commandBuilder.geoadd(key, lngLatMember));
    }

    @Override
    public RedisFuture<Double> geodist(K key, V from, V to, GeoArgs.Unit unit) {
        return dispatch(commandBuilder.geodist(key, from, to, unit));
    }

    @Override
    public RedisFuture<List<Value<String>>> geohash(K key, V... members) {
        return dispatch(commandBuilder.geohash(key, members));
    }

    @Override
    public RedisFuture<List<GeoCoordinates>> geopos(K key, V... members) {
        return dispatch(commandBuilder.geopos(key, members));
    }

    @Override
    public RedisFuture<Set<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {
        return dispatch(commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance, unit.name()));
    }

    @Override
    public RedisFuture<List<GeoWithin<V>>> georadius(K key, double longitude, double latitude, double distance,
            GeoArgs.Unit unit, GeoArgs geoArgs) {
        return dispatch(commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance, unit.name(), geoArgs));
    }

    @Override
    public RedisFuture<Long> georadius(K key, double longitude, double latitude, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return dispatch(commandBuilder.georadius(key, longitude, latitude, distance, unit.name(), geoRadiusStoreArgs));
    }

    protected RedisFuture<Set<V>> georadius_ro(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {
        return dispatch(commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance, unit.name()));
    }

    protected RedisFuture<List<GeoWithin<V>>> georadius_ro(K key, double longitude, double latitude, double distance,
            GeoArgs.Unit unit, GeoArgs geoArgs) {
        return dispatch(commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance, unit.name(), geoArgs));
    }

    @Override
    public RedisFuture<Set<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {
        return dispatch(commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance, unit.name()));
    }

    @Override
    public RedisFuture<List<GeoWithin<V>>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {
        return dispatch(commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance, unit.name(), geoArgs));
    }

    @Override
    public RedisFuture<Long> georadiusbymember(K key, V member, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return dispatch(commandBuilder.georadiusbymember(key, member, distance, unit.name(), geoRadiusStoreArgs));
    }

    protected RedisFuture<Set<V>> georadiusbymember_ro(K key, V member, double distance, GeoArgs.Unit unit) {
        return dispatch(commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance, unit.name()));
    }

    protected RedisFuture<List<GeoWithin<V>>> georadiusbymember_ro(K key, V member, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {
        return dispatch(commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance, unit.name(), geoArgs));
    }

    @Override
    public RedisFuture<V> get(K key) {
        return dispatch(commandBuilder.get(key));
    }

    public StatefulConnection<K, V> getConnection() {
        return connection;
    }

    @Override
    public RedisFuture<Long> getbit(K key, long offset) {
        return dispatch(commandBuilder.getbit(key, offset));
    }

    @Override
    public RedisFuture<V> getrange(K key, long start, long end) {
        return dispatch(commandBuilder.getrange(key, start, end));
    }

    @Override
    public RedisFuture<V> getset(K key, V value) {
        return dispatch(commandBuilder.getset(key, value));
    }

    @Override
    public RedisFuture<Long> hdel(K key, K... fields) {
        return dispatch(commandBuilder.hdel(key, fields));
    }

    @Override
    public RedisFuture<Boolean> hexists(K key, K field) {
        return dispatch(commandBuilder.hexists(key, field));
    }

    @Override
    public RedisFuture<V> hget(K key, K field) {
        return dispatch(commandBuilder.hget(key, field));
    }

    @Override
    public RedisFuture<Map<K, V>> hgetall(K key) {
        return dispatch(commandBuilder.hgetall(key));
    }

    @Override
    public RedisFuture<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        return dispatch(commandBuilder.hgetall(channel, key));
    }

    @Override
    public RedisFuture<Long> hincrby(K key, K field, long amount) {
        return dispatch(commandBuilder.hincrby(key, field, amount));
    }

    @Override
    public RedisFuture<Double> hincrbyfloat(K key, K field, double amount) {
        return dispatch(commandBuilder.hincrbyfloat(key, field, amount));
    }

    @Override
    public RedisFuture<List<K>> hkeys(K key) {
        return dispatch(commandBuilder.hkeys(key));
    }

    @Override
    public RedisFuture<Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        return dispatch(commandBuilder.hkeys(channel, key));
    }

    @Override
    public RedisFuture<Long> hlen(K key) {
        return dispatch(commandBuilder.hlen(key));
    }

    @Override
    public RedisFuture<List<KeyValue<K, V>>> hmget(K key, K... fields) {
        return dispatch(commandBuilder.hmgetKeyValue(key, fields));
    }

    @Override
    public RedisFuture<Long> hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        return dispatch(commandBuilder.hmget(channel, key, fields));
    }

    @Override
    public RedisFuture<String> hmset(K key, Map<K, V> map) {
        return dispatch(commandBuilder.hmset(key, map));
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key) {
        return dispatch(commandBuilder.hscan(key));
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.hscan(key, scanArgs));
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return dispatch(commandBuilder.hscan(key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.hscan(key, scanCursor));
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return dispatch(commandBuilder.hscanStreaming(channel, key));
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.hscanStreaming(channel, key, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return dispatch(commandBuilder.hscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.hscanStreaming(channel, key, scanCursor));
    }

    @Override
    public RedisFuture<Boolean> hset(K key, K field, V value) {
        return dispatch(commandBuilder.hset(key, field, value));
    }

    @Override
    public RedisFuture<Long> hset(K key, Map<K, V> map) {
        return dispatch(commandBuilder.hset(key, map));
    }

    @Override
    public RedisFuture<Boolean> hsetnx(K key, K field, V value) {
        return dispatch(commandBuilder.hsetnx(key, field, value));
    }

    @Override
    public RedisFuture<Long> hstrlen(K key, K field) {
        return dispatch(commandBuilder.hstrlen(key, field));
    }

    @Override
    public RedisFuture<List<V>> hvals(K key) {
        return dispatch(commandBuilder.hvals(key));
    }

    @Override
    public RedisFuture<Long> hvals(ValueStreamingChannel<V> channel, K key) {
        return dispatch(commandBuilder.hvals(channel, key));
    }

    @Override
    public RedisFuture<Long> incr(K key) {
        return dispatch(commandBuilder.incr(key));
    }

    @Override
    public RedisFuture<Long> incrby(K key, long amount) {
        return dispatch(commandBuilder.incrby(key, amount));
    }

    @Override
    public RedisFuture<Double> incrbyfloat(K key, double amount) {
        return dispatch(commandBuilder.incrbyfloat(key, amount));
    }

    @Override
    public RedisFuture<String> info() {
        return dispatch(commandBuilder.info());
    }

    @Override
    public RedisFuture<String> info(String section) {
        return dispatch(commandBuilder.info(section));
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public RedisFuture<List<K>> keys(K pattern) {
        return dispatch(commandBuilder.keys(pattern));
    }

    @Override
    public RedisFuture<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        return dispatch(commandBuilder.keys(channel, pattern));
    }

    @Override
    public RedisFuture<Date> lastsave() {
        return dispatch(commandBuilder.lastsave());
    }

    @Override
    public RedisFuture<V> lindex(K key, long index) {
        return dispatch(commandBuilder.lindex(key, index));
    }

    @Override
    public RedisFuture<Long> linsert(K key, boolean before, V pivot, V value) {
        return dispatch(commandBuilder.linsert(key, before, pivot, value));
    }

    @Override
    public RedisFuture<Long> llen(K key) {
        return dispatch(commandBuilder.llen(key));
    }

    @Override
    public RedisFuture<V> lpop(K key) {
        return dispatch(commandBuilder.lpop(key));
    }

    @Override
    public RedisFuture<Long> lpos(K key, V value) {
        return lpos(key, value, null);
    }

    @Override
    public RedisFuture<Long> lpos(K key, V value, LPosArgs args) {
        return dispatch(commandBuilder.lpos(key, value, args));
    }

    @Override
    public RedisFuture<List<Long>> lpos(K key, V value, int count) {
        return lpos(key, value, count, null);
    }

    @Override
    public RedisFuture<List<Long>> lpos(K key, V value, int count, LPosArgs args) {
        return dispatch(commandBuilder.lpos(key, value, count, args));
    }

    @Override
    public RedisFuture<Long> lpush(K key, V... values) {
        return dispatch(commandBuilder.lpush(key, values));
    }

    @Override
    public RedisFuture<Long> lpushx(K key, V... values) {
        return dispatch(commandBuilder.lpushx(key, values));
    }

    @Override
    public RedisFuture<List<V>> lrange(K key, long start, long stop) {
        return dispatch(commandBuilder.lrange(key, start, stop));
    }

    @Override
    public RedisFuture<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return dispatch(commandBuilder.lrange(channel, key, start, stop));
    }

    @Override
    public RedisFuture<Long> lrem(K key, long count, V value) {
        return dispatch(commandBuilder.lrem(key, count, value));
    }

    @Override
    public RedisFuture<String> lset(K key, long index, V value) {
        return dispatch(commandBuilder.lset(key, index, value));
    }

    @Override
    public RedisFuture<String> ltrim(K key, long start, long stop) {
        return dispatch(commandBuilder.ltrim(key, start, stop));
    }

    @Override
    public RedisFuture<Long> memoryUsage(K key) {
        return dispatch(commandBuilder.memoryUsage(key));
    }

    @Override
    public RedisFuture<List<KeyValue<K, V>>> mget(K... keys) {
        return dispatch(commandBuilder.mgetKeyValue(keys));
    }

    public RedisFuture<List<KeyValue<K, V>>> mget(Iterable<K> keys) {
        return dispatch(commandBuilder.mgetKeyValue(keys));
    }

    @Override
    public RedisFuture<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return dispatch(commandBuilder.mget(channel, keys));
    }

    public RedisFuture<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        return dispatch(commandBuilder.mget(channel, keys));
    }

    @Override
    public RedisFuture<String> migrate(String host, int port, K key, int db, long timeout) {
        return dispatch(commandBuilder.migrate(host, port, key, db, timeout));
    }

    @Override
    public RedisFuture<String> migrate(String host, int port, int db, long timeout, MigrateArgs<K> migrateArgs) {
        return dispatch(commandBuilder.migrate(host, port, db, timeout, migrateArgs));
    }

    @Override
    public RedisFuture<Boolean> move(K key, int db) {
        return dispatch(commandBuilder.move(key, db));
    }

    @Override
    public RedisFuture<String> mset(Map<K, V> map) {
        return dispatch(commandBuilder.mset(map));
    }

    @Override
    public RedisFuture<Boolean> msetnx(Map<K, V> map) {
        return dispatch(commandBuilder.msetnx(map));
    }

    @Override
    public RedisFuture<String> multi() {
        return dispatch(commandBuilder.multi());
    }

    @Override
    public RedisFuture<String> objectEncoding(K key) {
        return dispatch(commandBuilder.objectEncoding(key));
    }

    @Override
    public RedisFuture<Long> objectIdletime(K key) {
        return dispatch(commandBuilder.objectIdletime(key));
    }

    @Override
    public RedisFuture<Long> objectRefcount(K key) {
        return dispatch(commandBuilder.objectRefcount(key));
    }

    @Override
    public RedisFuture<Boolean> persist(K key) {
        return dispatch(commandBuilder.persist(key));
    }

    @Override
    public RedisFuture<Boolean> pexpire(K key, long milliseconds) {
        return dispatch(commandBuilder.pexpire(key, milliseconds));
    }

    @Override
    public RedisFuture<Boolean> pexpireat(K key, Date timestamp) {
        return pexpireat(key, timestamp.getTime());
    }

    @Override
    public RedisFuture<Boolean> pexpireat(K key, long timestamp) {
        return dispatch(commandBuilder.pexpireat(key, timestamp));
    }

    @Override
    public RedisFuture<Long> pfadd(K key, V... values) {
        return dispatch(commandBuilder.pfadd(key, values));
    }

    @Override
    public RedisFuture<Long> pfcount(K... keys) {
        return dispatch(commandBuilder.pfcount(keys));
    }

    @Override
    public RedisFuture<String> pfmerge(K destkey, K... sourcekeys) {
        return dispatch(commandBuilder.pfmerge(destkey, sourcekeys));
    }

    @Override
    public RedisFuture<String> ping() {
        return dispatch(commandBuilder.ping());
    }

    @Override
    public RedisFuture<String> psetex(K key, long milliseconds, V value) {
        return dispatch(commandBuilder.psetex(key, milliseconds, value));
    }

    @Override
    public RedisFuture<Long> pttl(K key) {
        return dispatch(commandBuilder.pttl(key));
    }

    @Override
    public RedisFuture<Long> publish(K channel, V message) {
        return dispatch(commandBuilder.publish(channel, message));
    }

    @Override
    public RedisFuture<List<K>> pubsubChannels() {
        return dispatch(commandBuilder.pubsubChannels());
    }

    @Override
    public RedisFuture<List<K>> pubsubChannels(K channel) {
        return dispatch(commandBuilder.pubsubChannels(channel));
    }

    @Override
    public RedisFuture<Long> pubsubNumpat() {
        return dispatch(commandBuilder.pubsubNumpat());
    }

    @Override
    public RedisFuture<Map<K, Long>> pubsubNumsub(K... channels) {
        return dispatch(commandBuilder.pubsubNumsub(channels));
    }

    @Override
    public RedisFuture<String> quit() {
        return dispatch(commandBuilder.quit());
    }

    @Override
    public RedisFuture<K> randomkey() {
        return dispatch(commandBuilder.randomkey());
    }

    @Override
    public RedisFuture<String> readOnly() {
        return dispatch(commandBuilder.readOnly());
    }

    @Override
    public RedisFuture<String> readWrite() {
        return dispatch(commandBuilder.readWrite());
    }

    @Override
    public RedisFuture<String> rename(K key, K newKey) {
        return dispatch(commandBuilder.rename(key, newKey));
    }

    @Override
    public RedisFuture<Boolean> renamenx(K key, K newKey) {
        return dispatch(commandBuilder.renamenx(key, newKey));
    }

    @Override
    public void reset() {
        getConnection().reset();
    }

    @Override
    public RedisFuture<String> restore(K key, long ttl, byte[] value) {
        return dispatch(commandBuilder.restore(key, value, RestoreArgs.Builder.ttl(ttl)));
    }

    @Override
    public RedisFuture<String> restore(K key, byte[] value, RestoreArgs args) {
        return dispatch(commandBuilder.restore(key, value, args));
    }

    @Override
    public RedisFuture<List<Object>> role() {
        return dispatch(commandBuilder.role());
    }

    @Override
    public RedisFuture<V> rpop(K key) {
        return dispatch(commandBuilder.rpop(key));
    }

    @Override
    public RedisFuture<V> rpoplpush(K source, K destination) {
        return dispatch(commandBuilder.rpoplpush(source, destination));
    }

    @Override
    public RedisFuture<Long> rpush(K key, V... values) {
        return dispatch(commandBuilder.rpush(key, values));
    }

    @Override
    public RedisFuture<Long> rpushx(K key, V... values) {
        return dispatch(commandBuilder.rpushx(key, values));
    }

    @Override
    public RedisFuture<Long> sadd(K key, V... members) {
        return dispatch(commandBuilder.sadd(key, members));
    }

    @Override
    public RedisFuture<String> save() {
        return dispatch(commandBuilder.save());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan() {
        return dispatch(commandBuilder.scan());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return dispatch(commandBuilder.scan(scanArgs));
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return dispatch(commandBuilder.scan(scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return dispatch(commandBuilder.scan(scanCursor));
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return dispatch(commandBuilder.scanStreaming(channel));
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return dispatch(commandBuilder.scanStreaming(channel, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return dispatch(commandBuilder.scanStreaming(channel, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return dispatch(commandBuilder.scanStreaming(channel, scanCursor));
    }

    @Override
    public RedisFuture<Long> scard(K key) {
        return dispatch(commandBuilder.scard(key));
    }

    @Override
    public RedisFuture<List<Boolean>> scriptExists(String... digests) {
        return dispatch(commandBuilder.scriptExists(digests));
    }

    @Override
    public RedisFuture<String> scriptFlush() {
        return dispatch(commandBuilder.scriptFlush());
    }

    @Override
    public RedisFuture<String> scriptKill() {
        return dispatch(commandBuilder.scriptKill());
    }

    @Override
    public RedisFuture<String> scriptLoad(V script) {
        return dispatch(commandBuilder.scriptLoad(script));
    }

    @Override
    public RedisFuture<Set<V>> sdiff(K... keys) {
        return dispatch(commandBuilder.sdiff(keys));
    }

    @Override
    public RedisFuture<Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        return dispatch(commandBuilder.sdiff(channel, keys));
    }

    @Override
    public RedisFuture<Long> sdiffstore(K destination, K... keys) {
        return dispatch(commandBuilder.sdiffstore(destination, keys));
    }

    public String select(int db) {
        AsyncCommand<K, V, String> cmd = selectAsync(db);
        return LettuceFutures.awaitOrCancel(cmd, connection.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    protected AsyncCommand<K, V, String> selectAsync(int db) {
        return dispatch(commandBuilder.select(db));
    }

    @Override
    public RedisFuture<String> set(K key, V value) {
        return dispatch(commandBuilder.set(key, value));
    }

    @Override
    public RedisFuture<String> set(K key, V value, SetArgs setArgs) {
        return dispatch(commandBuilder.set(key, value, setArgs));
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        connection.setAutoFlushCommands(autoFlush);
    }

    public void setTimeout(Duration timeout) {
        connection.setTimeout(timeout);
    }

    @Deprecated
    public void setTimeout(long timeout, TimeUnit unit) {
        connection.setTimeout(timeout, unit);
    }

    @Override
    public RedisFuture<Long> setbit(K key, long offset, int value) {
        return dispatch(commandBuilder.setbit(key, offset, value));
    }

    @Override
    public RedisFuture<String> setex(K key, long seconds, V value) {
        return dispatch(commandBuilder.setex(key, seconds, value));
    }

    @Override
    public RedisFuture<Boolean> setnx(K key, V value) {
        return dispatch(commandBuilder.setnx(key, value));
    }

    @Override
    public RedisFuture<Long> setrange(K key, long offset, V value) {
        return dispatch(commandBuilder.setrange(key, offset, value));
    }

    @Override
    public void shutdown(boolean save) {
        dispatch(commandBuilder.shutdown(save));
    }

    @Override
    public RedisFuture<Set<V>> sinter(K... keys) {
        return dispatch(commandBuilder.sinter(keys));
    }

    @Override
    public RedisFuture<Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        return dispatch(commandBuilder.sinter(channel, keys));
    }

    @Override
    public RedisFuture<Long> sinterstore(K destination, K... keys) {
        return dispatch(commandBuilder.sinterstore(destination, keys));
    }

    @Override
    public RedisFuture<Boolean> sismember(K key, V member) {
        return dispatch(commandBuilder.sismember(key, member));
    }

    @Override
    public RedisFuture<String> slaveof(String host, int port) {
        return dispatch(commandBuilder.slaveof(host, port));
    }

    @Override
    public RedisFuture<String> slaveofNoOne() {
        return dispatch(commandBuilder.slaveofNoOne());
    }

    @Override
    public RedisFuture<List<Object>> slowlogGet() {
        return dispatch(commandBuilder.slowlogGet());
    }

    @Override
    public RedisFuture<List<Object>> slowlogGet(int count) {
        return dispatch(commandBuilder.slowlogGet(count));
    }

    @Override
    public RedisFuture<Long> slowlogLen() {
        return dispatch(commandBuilder.slowlogLen());
    }

    @Override
    public RedisFuture<String> slowlogReset() {
        return dispatch(commandBuilder.slowlogReset());
    }

    @Override
    public RedisFuture<Set<V>> smembers(K key) {
        return dispatch(commandBuilder.smembers(key));
    }

    @Override
    public RedisFuture<Long> smembers(ValueStreamingChannel<V> channel, K key) {
        return dispatch(commandBuilder.smembers(channel, key));
    }

    @Override
    public RedisFuture<Boolean> smove(K source, K destination, V member) {
        return dispatch(commandBuilder.smove(source, destination, member));
    }

    @Override
    public RedisFuture<List<V>> sort(K key) {
        return dispatch(commandBuilder.sort(key));
    }

    @Override
    public RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key) {
        return dispatch(commandBuilder.sort(channel, key));
    }

    @Override
    public RedisFuture<List<V>> sort(K key, SortArgs sortArgs) {
        return dispatch(commandBuilder.sort(key, sortArgs));
    }

    @Override
    public RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        return dispatch(commandBuilder.sort(channel, key, sortArgs));
    }

    @Override
    public RedisFuture<Long> sortStore(K key, SortArgs sortArgs, K destination) {
        return dispatch(commandBuilder.sortStore(key, sortArgs, destination));
    }

    @Override
    public RedisFuture<V> spop(K key) {
        return dispatch(commandBuilder.spop(key));
    }

    @Override
    public RedisFuture<Set<V>> spop(K key, long count) {
        return dispatch(commandBuilder.spop(key, count));
    }

    @Override
    public RedisFuture<V> srandmember(K key) {
        return dispatch(commandBuilder.srandmember(key));
    }

    @Override
    public RedisFuture<List<V>> srandmember(K key, long count) {
        return dispatch(commandBuilder.srandmember(key, count));
    }

    @Override
    public RedisFuture<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        return dispatch(commandBuilder.srandmember(channel, key, count));
    }

    @Override
    public RedisFuture<Long> srem(K key, V... members) {
        return dispatch(commandBuilder.srem(key, members));
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key) {
        return dispatch(commandBuilder.sscan(key));
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.sscan(key, scanArgs));
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return dispatch(commandBuilder.sscan(key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.sscan(key, scanCursor));
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return dispatch(commandBuilder.sscanStreaming(channel, key));
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.sscanStreaming(channel, key, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return dispatch(commandBuilder.sscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.sscanStreaming(channel, key, scanCursor));
    }

    @Override
    public RedisFuture<Long> strlen(K key) {
        return dispatch(commandBuilder.strlen(key));
    }

    @Override
    public RedisFuture<StringMatchResult> stralgoLcs(StrAlgoArgs args) {
        return dispatch(commandBuilder.stralgoLcs(args));
    }

    @Override
    public RedisFuture<Set<V>> sunion(K... keys) {
        return dispatch(commandBuilder.sunion(keys));
    }

    @Override
    public RedisFuture<Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        return dispatch(commandBuilder.sunion(channel, keys));
    }

    @Override
    public RedisFuture<Long> sunionstore(K destination, K... keys) {
        return dispatch(commandBuilder.sunionstore(destination, keys));
    }

    public RedisFuture<String> swapdb(int db1, int db2) {
        return dispatch(commandBuilder.swapdb(db1, db2));
    }

    @Override
    public RedisFuture<List<V>> time() {
        return dispatch(commandBuilder.time());
    }

    @Override
    public RedisFuture<Long> touch(K... keys) {
        return dispatch(commandBuilder.touch(keys));
    }

    public RedisFuture<Long> touch(Iterable<K> keys) {
        return dispatch(commandBuilder.touch(keys));
    }

    @Override
    public RedisFuture<Long> ttl(K key) {
        return dispatch(commandBuilder.ttl(key));
    }

    @Override
    public RedisFuture<String> type(K key) {
        return dispatch(commandBuilder.type(key));
    }

    @Override
    public RedisFuture<Long> unlink(K... keys) {
        return dispatch(commandBuilder.unlink(keys));
    }

    public RedisFuture<Long> unlink(Iterable<K> keys) {
        return dispatch(commandBuilder.unlink(keys));
    }

    @Override
    public RedisFuture<String> unwatch() {
        return dispatch(commandBuilder.unwatch());
    }

    @Override
    public RedisFuture<Long> waitForReplication(int replicas, long timeout) {
        return dispatch(commandBuilder.wait(replicas, timeout));
    }

    @Override
    public RedisFuture<String> watch(K... keys) {
        return dispatch(commandBuilder.watch(keys));
    }

    @Override
    public RedisFuture<Long> xack(K key, K group, String... messageIds) {
        return dispatch(commandBuilder.xack(key, group, messageIds));
    }

    @Override
    public RedisFuture<String> xadd(K key, Map<K, V> body) {
        return dispatch(commandBuilder.xadd(key, null, body));
    }

    @Override
    public RedisFuture<String> xadd(K key, XAddArgs args, Map<K, V> body) {
        return dispatch(commandBuilder.xadd(key, args, body));
    }

    @Override
    public RedisFuture<String> xadd(K key, Object... keysAndValues) {
        return dispatch(commandBuilder.xadd(key, null, keysAndValues));
    }

    @Override
    public RedisFuture<String> xadd(K key, XAddArgs args, Object... keysAndValues) {
        return dispatch(commandBuilder.xadd(key, args, keysAndValues));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xclaim(K key, Consumer<K> consumer, long minIdleTime, String... messageIds) {
        return dispatch(commandBuilder.xclaim(key, consumer, XClaimArgs.Builder.minIdleTime(minIdleTime), messageIds));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xclaim(K key, Consumer<K> consumer, XClaimArgs args, String... messageIds) {
        return dispatch(commandBuilder.xclaim(key, consumer, args, messageIds));
    }

    @Override
    public RedisFuture<Long> xdel(K key, String... messageIds) {
        return dispatch(commandBuilder.xdel(key, messageIds));
    }

    @Override
    public RedisFuture<String> xgroupCreate(XReadArgs.StreamOffset<K> offset, K group) {
        return dispatch(commandBuilder.xgroupCreate(offset, group, null));
    }

    @Override
    public RedisFuture<String> xgroupCreate(XReadArgs.StreamOffset<K> offset, K group, XGroupCreateArgs args) {
        return dispatch(commandBuilder.xgroupCreate(offset, group, args));
    }

    @Override
    public RedisFuture<Boolean> xgroupDelconsumer(K key, Consumer<K> consumer) {
        return dispatch(commandBuilder.xgroupDelconsumer(key, consumer));
    }

    @Override
    public RedisFuture<Boolean> xgroupDestroy(K key, K group) {
        return dispatch(commandBuilder.xgroupDestroy(key, group));
    }

    @Override
    public RedisFuture<String> xgroupSetid(XReadArgs.StreamOffset<K> offset, K group) {
        return dispatch(commandBuilder.xgroupSetid(offset, group));
    }

    @Override
    public RedisFuture<List<Object>> xinfoStream(K key) {
        return dispatch(commandBuilder.xinfoStream(key));
    }

    @Override
    public RedisFuture<List<Object>> xinfoGroups(K key) {
        return dispatch(commandBuilder.xinfoGroups(key));
    }

    @Override
    public RedisFuture<List<Object>> xinfoConsumers(K key, K group) {
        return dispatch(commandBuilder.xinfoConsumers(key, group));
    }

    @Override
    public RedisFuture<Long> xlen(K key) {
        return dispatch(commandBuilder.xlen(key));
    }

    @Override
    public RedisFuture<List<Object>> xpending(K key, K group) {
        return dispatch(commandBuilder.xpending(key, group, Range.unbounded(), Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<Object>> xpending(K key, K group, Range<String> range, Limit limit) {
        return dispatch(commandBuilder.xpending(key, group, range, limit));
    }

    @Override
    public RedisFuture<List<Object>> xpending(K key, Consumer<K> consumer, Range<String> range, Limit limit) {
        return dispatch(commandBuilder.xpending(key, consumer, range, limit));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xrange(K key, Range<String> range) {
        return dispatch(commandBuilder.xrange(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xrange(K key, Range<String> range, Limit limit) {
        return dispatch(commandBuilder.xrange(key, range, limit));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xread(XReadArgs.StreamOffset<K>... streams) {
        return dispatch(commandBuilder.xread(null, streams));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xread(XReadArgs args, XReadArgs.StreamOffset<K>... streams) {
        return dispatch(commandBuilder.xread(args, streams));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xreadgroup(Consumer<K> consumer, XReadArgs.StreamOffset<K>... streams) {
        return dispatch(commandBuilder.xreadgroup(consumer, null, streams));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xreadgroup(Consumer<K> consumer, XReadArgs args,
            XReadArgs.StreamOffset<K>... streams) {
        return dispatch(commandBuilder.xreadgroup(consumer, args, streams));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xrevrange(K key, Range<String> range) {
        return dispatch(commandBuilder.xrevrange(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<StreamMessage<K, V>>> xrevrange(K key, Range<String> range, Limit limit) {
        return dispatch(commandBuilder.xrevrange(key, range, limit));
    }

    @Override
    public RedisFuture<Long> xtrim(K key, long count) {
        return xtrim(key, false, count);
    }

    @Override
    public RedisFuture<Long> xtrim(K key, boolean approximateTrimming, long count) {
        return dispatch(commandBuilder.xtrim(key, approximateTrimming, count));
    }

    @Override
    public RedisFuture<KeyValue<K, ScoredValue<V>>> bzpopmin(long timeout, K... keys) {
        return dispatch(commandBuilder.bzpopmin(timeout, keys));
    }

    @Override
    public RedisFuture<KeyValue<K, ScoredValue<V>>> bzpopmax(long timeout, K... keys) {
        return dispatch(commandBuilder.bzpopmax(timeout, keys));
    }

    @Override
    public RedisFuture<Long> zadd(K key, double score, V member) {
        return dispatch(commandBuilder.zadd(key, null, score, member));
    }

    @Override
    public RedisFuture<Long> zadd(K key, Object... scoresAndValues) {
        return dispatch(commandBuilder.zadd(key, null, scoresAndValues));
    }

    @Override
    public RedisFuture<Long> zadd(K key, ScoredValue<V>... scoredValues) {
        return dispatch(commandBuilder.zadd(key, null, (Object[]) scoredValues));
    }

    @Override
    public RedisFuture<Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return dispatch(commandBuilder.zadd(key, zAddArgs, score, member));
    }

    @Override
    public RedisFuture<Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        return dispatch(commandBuilder.zadd(key, zAddArgs, scoresAndValues));
    }

    @Override
    public RedisFuture<Long> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... scoredValues) {
        return dispatch(commandBuilder.zadd(key, zAddArgs, (Object[]) scoredValues));
    }

    @Override
    public RedisFuture<Double> zaddincr(K key, double score, V member) {
        return dispatch(commandBuilder.zaddincr(key, null, score, member));
    }

    @Override
    public RedisFuture<Double> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        return dispatch(commandBuilder.zaddincr(key, zAddArgs, score, member));
    }

    @Override
    public RedisFuture<Long> zcard(K key) {
        return dispatch(commandBuilder.zcard(key));
    }

    @Override
    public RedisFuture<Long> zcount(K key, double min, double max) {
        return dispatch(commandBuilder.zcount(key, min, max));
    }

    @Override
    public RedisFuture<Long> zcount(K key, String min, String max) {
        return dispatch(commandBuilder.zcount(key, min, max));
    }

    @Override
    public RedisFuture<Long> zcount(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zcount(key, range));
    }

    @Override
    public RedisFuture<Double> zincrby(K key, double amount, V member) {
        return dispatch(commandBuilder.zincrby(key, amount, member));
    }

    @Override
    public RedisFuture<Long> zinterstore(K destination, K... keys) {
        return dispatch(commandBuilder.zinterstore(destination, keys));
    }

    @Override
    public RedisFuture<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return dispatch(commandBuilder.zinterstore(destination, storeArgs, keys));
    }

    @Override
    public RedisFuture<Long> zlexcount(K key, String min, String max) {
        return dispatch(commandBuilder.zlexcount(key, min, max));
    }

    @Override
    public RedisFuture<Long> zlexcount(K key, Range<? extends V> range) {
        return dispatch(commandBuilder.zlexcount(key, range));
    }

    @Override
    public RedisFuture<ScoredValue<V>> zpopmin(K key) {
        return dispatch(commandBuilder.zpopmin(key));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zpopmin(K key, long count) {
        return dispatch(commandBuilder.zpopmin(key, count));
    }

    @Override
    public RedisFuture<ScoredValue<V>> zpopmax(K key) {
        return dispatch(commandBuilder.zpopmax(key));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zpopmax(K key, long count) {
        return dispatch(commandBuilder.zpopmax(key, count));
    }

    @Override
    public RedisFuture<List<V>> zrange(K key, long start, long stop) {
        return dispatch(commandBuilder.zrange(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return dispatch(commandBuilder.zrange(channel, key, start, stop));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop) {
        return dispatch(commandBuilder.zrangeWithScores(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return dispatch(commandBuilder.zrangeWithScores(channel, key, start, stop));
    }

    @Override
    public RedisFuture<List<V>> zrangebylex(K key, String min, String max) {
        return dispatch(commandBuilder.zrangebylex(key, min, max));
    }

    @Override
    public RedisFuture<List<V>> zrangebylex(K key, Range<? extends V> range) {
        return dispatch(commandBuilder.zrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<V>> zrangebylex(K key, String min, String max, long offset, long count) {
        return dispatch(commandBuilder.zrangebylex(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<List<V>> zrangebylex(K key, Range<? extends V> range, Limit limit) {
        return dispatch(commandBuilder.zrangebylex(key, range, limit));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, double min, double max) {
        return dispatch(commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, String min, String max) {
        return dispatch(commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrangebyscore(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, double min, double max, long offset, long count) {
        return dispatch(commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, String min, String max, long offset, long count) {
        return dispatch(commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<List<V>> zrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrangebyscore(key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset,
            long count) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset,
            long count) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        return dispatch(commandBuilder.zrangebyscore(channel, key, range, limit));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrank(K key, V member) {
        return dispatch(commandBuilder.zrank(key, member));
    }

    @Override
    public RedisFuture<Long> zrem(K key, V... members) {
        return dispatch(commandBuilder.zrem(key, members));
    }

    @Override
    public RedisFuture<Long> zremrangebylex(K key, String min, String max) {
        return dispatch(commandBuilder.zremrangebylex(key, min, max));
    }

    @Override
    public RedisFuture<Long> zremrangebylex(K key, Range<? extends V> range) {
        return dispatch(commandBuilder.zremrangebylex(key, range));
    }

    @Override
    public RedisFuture<Long> zremrangebyrank(K key, long start, long stop) {
        return dispatch(commandBuilder.zremrangebyrank(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zremrangebyscore(K key, double min, double max) {
        return dispatch(commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public RedisFuture<Long> zremrangebyscore(K key, String min, String max) {
        return dispatch(commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public RedisFuture<Long> zremrangebyscore(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zremrangebyscore(key, range));
    }

    @Override
    public RedisFuture<List<V>> zrevrange(K key, long start, long stop) {
        return dispatch(commandBuilder.zrevrange(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return dispatch(commandBuilder.zrevrange(channel, key, start, stop));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop) {
        return dispatch(commandBuilder.zrevrangeWithScores(key, start, stop));
    }

    @Override
    public RedisFuture<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return dispatch(commandBuilder.zrevrangeWithScores(channel, key, start, stop));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebylex(K key, Range<? extends V> range) {
        return dispatch(commandBuilder.zrevrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebylex(K key, Range<? extends V> range, Limit limit) {
        return dispatch(commandBuilder.zrevrangebylex(key, range, limit));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min) {
        return dispatch(commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min) {
        return dispatch(commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrevrangebyscore(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return dispatch(commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        return dispatch(commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public RedisFuture<List<V>> zrevrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrevrangebyscore(key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        return dispatch(commandBuilder.zrevrangebyscore(channel, key, range, limit));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset,
            long count) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset,
            long count) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        return dispatch(commandBuilder.zrevrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public RedisFuture<Long> zrevrank(K key, V member) {
        return dispatch(commandBuilder.zrevrank(key, member));
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key) {
        return dispatch(commandBuilder.zscan(key));
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.zscan(key, scanArgs));
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return dispatch(commandBuilder.zscan(key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.zscan(key, scanCursor));
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return dispatch(commandBuilder.zscanStreaming(channel, key));
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return dispatch(commandBuilder.zscanStreaming(channel, key, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return dispatch(commandBuilder.zscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return dispatch(commandBuilder.zscanStreaming(channel, key, scanCursor));
    }

    @Override
    public RedisFuture<Double> zscore(K key, V member) {
        return dispatch(commandBuilder.zscore(key, member));
    }

    @Override
    public RedisFuture<Long> zunionstore(K destination, K... keys) {
        return dispatch(commandBuilder.zunionstore(destination, keys));
    }

    @Override
    public RedisFuture<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return dispatch(commandBuilder.zunionstore(destination, storeArgs, keys));
    }

}
