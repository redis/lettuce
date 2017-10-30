/*
 * Copyright 2011-2018 the original author or authors.
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
package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandType.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.GeoArgs.Unit;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.rx.*;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.*;
import com.lambdaworks.redis.protocol.*;

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

    protected StatefulConnection<K, V> connection;
    protected RedisCodec<K, V> codec;
    protected RedisCommandBuilder<K, V> commandBuilder;
    protected MultiOutput<K, V> multi;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     */
    public AbstractRedisReactiveCommands(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        this.codec = codec;
        commandBuilder = new RedisCommandBuilder<>(codec);
    }

    @Override
    public Observable<Long> append(K key, V value) {
        return createObservable(() -> commandBuilder.append(key, value));
    }

    @Override
    public Observable<String> asking() {
        return createObservable(commandBuilder::asking);
    }

    @Override
    public Observable<String> auth(String password) {
        return createObservable(() -> commandBuilder.auth(password));
    }

    @Override
    public Observable<String> bgrewriteaof() {
        return createObservable(commandBuilder::bgrewriteaof);
    }

    @Override
    public Observable<String> bgsave() {
        return createObservable(commandBuilder::bgsave);
    }

    @Override
    public Observable<Long> bitcount(K key) {
        return createObservable(() -> commandBuilder.bitcount(key));
    }

    @Override
    public Observable<Long> bitcount(K key, long start, long end) {
        return createObservable(() -> commandBuilder.bitcount(key, start, end));
    }

    @Override
    public Observable<Long> bitfield(K key, BitFieldArgs args) {
        return createDissolvingObservable(() -> commandBuilder.bitfield(key, args));
    }

    @Override
    public Observable<Long> bitopAnd(K destination, K... keys) {
        return createObservable(() -> commandBuilder.bitopAnd(destination, keys));
    }

    @Override
    public Observable<Long> bitopNot(K destination, K source) {
        return createObservable(() -> commandBuilder.bitopNot(destination, source));
    }

    @Override
    public Observable<Long> bitopOr(K destination, K... keys) {
        return createObservable(() -> commandBuilder.bitopOr(destination, keys));
    }

    @Override
    public Observable<Long> bitopXor(K destination, K... keys) {
        return createObservable(() -> commandBuilder.bitopXor(destination, keys));
    }

    @Override
    public Observable<Long> bitpos(K key, boolean state) {
        return createObservable(() -> commandBuilder.bitpos(key, state));
    }

    @Override
    public Observable<Long> bitpos(K key, boolean state, long start) {
        return createObservable(() -> commandBuilder.bitpos(key, state, start));
    }

    @Override
    public Observable<Long> bitpos(K key, boolean state, long start, long end) {
        return createObservable(() -> commandBuilder.bitpos(key, state, start, end));
    }

    @Override
    public Observable<KeyValue<K, V>> blpop(long timeout, K... keys) {
        return createObservable(() -> commandBuilder.blpop(timeout, keys));
    }

    @Override
    public Observable<KeyValue<K, V>> brpop(long timeout, K... keys) {
        return createObservable(() -> commandBuilder.brpop(timeout, keys));
    }

    @Override
    public Observable<V> brpoplpush(long timeout, K source, K destination) {
        return createObservable(() -> commandBuilder.brpoplpush(timeout, source, destination));
    }

    @Override
    public Observable<K> clientGetname() {
        return createObservable(commandBuilder::clientGetname);
    }

    @Override
    public Observable<String> clientKill(String addr) {
        return createObservable(() -> commandBuilder.clientKill(addr));
    }

    @Override
    public Observable<Long> clientKill(KillArgs killArgs) {
        return createObservable(() -> commandBuilder.clientKill(killArgs));
    }

    @Override
    public Observable<String> clientList() {
        return createObservable(commandBuilder::clientList);
    }

    @Override
    public Observable<String> clientPause(long timeout) {
        return createObservable(() -> commandBuilder.clientPause(timeout));
    }

    @Override
    public Observable<String> clientSetname(K name) {
        return createObservable(() -> commandBuilder.clientSetname(name));
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public Observable<String> clusterAddSlots(int... slots) {
        return createObservable(() -> commandBuilder.clusterAddslots(slots));
    }

    @Override
    public Observable<String> clusterBumpepoch() {
        return createObservable(() -> commandBuilder.clusterBumpepoch());
    }

    @Override
    public Observable<Long> clusterCountFailureReports(String nodeId) {
        return createObservable(() -> commandBuilder.clusterCountFailureReports(nodeId));
    }

    @Override
    public Observable<Long> clusterCountKeysInSlot(int slot) {
        return createObservable(() -> commandBuilder.clusterCountKeysInSlot(slot));
    }

    @Override
    public Observable<String> clusterDelSlots(int... slots) {
        return createObservable(() -> commandBuilder.clusterDelslots(slots));
    }

    @Override
    public Observable<String> clusterFailover(boolean force) {
        return createObservable(() -> commandBuilder.clusterFailover(force));
    }

    @Override
    public Observable<String> clusterFlushslots() {
        return createObservable(commandBuilder::clusterFlushslots);
    }

    @Override
    public Observable<String> clusterForget(String nodeId) {
        return createObservable(() -> commandBuilder.clusterForget(nodeId));
    }

    @Override
    public Observable<K> clusterGetKeysInSlot(int slot, int count) {
        return createDissolvingObservable(() -> commandBuilder.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public Observable<String> clusterInfo() {
        return createObservable(commandBuilder::clusterInfo);
    }

    @Override
    public Observable<Long> clusterKeyslot(K key) {
        return createObservable(() -> commandBuilder.clusterKeyslot(key));
    }

    @Override
    public Observable<String> clusterMeet(String ip, int port) {
        return createObservable(() -> commandBuilder.clusterMeet(ip, port));
    }

    @Override
    public Observable<String> clusterMyId() {
        return createObservable(commandBuilder::clusterMyId);
    }

    @Override
    public Observable<String> clusterNodes() {
        return createObservable(commandBuilder::clusterNodes);
    }

    @Override
    public Observable<String> clusterReplicate(String nodeId) {
        return createObservable(() -> commandBuilder.clusterReplicate(nodeId));
    }

    @Override
    public Observable<String> clusterReset(boolean hard) {
        return createObservable(() -> commandBuilder.clusterReset(hard));
    }

    @Override
    public Observable<String> clusterSaveconfig() {
        return createObservable(() -> commandBuilder.clusterSaveconfig());
    }

    @Override
    public Observable<String> clusterSetConfigEpoch(long configEpoch) {
        return createObservable(() -> commandBuilder.clusterSetConfigEpoch(configEpoch));
    }

    @Override
    public Observable<String> clusterSetSlotImporting(int slot, String nodeId) {
        return createObservable(() -> commandBuilder.clusterSetSlotImporting(slot, nodeId));
    }

    @Override
    public Observable<String> clusterSetSlotMigrating(int slot, String nodeId) {
        return createObservable(() -> commandBuilder.clusterSetSlotMigrating(slot, nodeId));
    }

    @Override
    public Observable<String> clusterSetSlotNode(int slot, String nodeId) {
        return createObservable(() -> commandBuilder.clusterSetSlotNode(slot, nodeId));
    }

    @Override
    public Observable<String> clusterSetSlotStable(int slot) {
        return createObservable(() -> commandBuilder.clusterSetSlotStable(slot));
    }

    @Override
    public Observable<String> clusterSlaves(String nodeId) {
        return createDissolvingObservable(() -> commandBuilder.clusterSlaves(nodeId));
    }

    @Override
    public Observable<Object> clusterSlots() {
        return createDissolvingObservable(commandBuilder::clusterSlots);
    }

    @Override
    public Observable<Object> command() {
        return createDissolvingObservable(commandBuilder::command);
    }

    @Override
    public Observable<Long> commandCount() {
        return createObservable(commandBuilder::commandCount);
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
    public Observable<String> configGet(String parameter) {
        return createDissolvingObservable(() -> commandBuilder.configGet(parameter));
    }

    @Override
    public Observable<String> configResetstat() {
        return createObservable(commandBuilder::configResetstat);
    }

    @Override
    public Observable<String> configRewrite() {
        return createObservable(commandBuilder::configRewrite);
    }

    @Override
    public Observable<String> configSet(String parameter, String value) {
        return createObservable(() -> commandBuilder.configSet(parameter, value));
    }

    @SuppressWarnings("unchecked")
    public <T, R> R createDissolvingObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return (R) Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, true));
    }

    @SuppressWarnings("unchecked")
    public <T, R> R createDissolvingObservable(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return (R) Observable
                .create(new ReactiveCommandDispatcher<>(() -> new Command<>(type, output, args), connection, true));
    }

    protected <T> Observable<T> createObservable(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return createObservable(() -> new Command<>(type, output, args));
    }

    public <T> Observable<T> createObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, false));
    }

    @Override
    public Observable<Long> dbsize() {
        return createObservable(commandBuilder::dbsize);
    }

    @Override
    public Observable<String> debugCrashAndRecover(Long delay) {
        return createObservable(() -> (commandBuilder.debugCrashAndRecover(delay)));
    }

    @Override
    public Observable<String> debugHtstats(int db) {
        return createObservable(() -> commandBuilder.debugHtstats(db));
    }

    @Override
    public Observable<String> debugObject(K key) {
        return createObservable(() -> commandBuilder.debugObject(key));
    }

    @Override
    public Observable<Success> debugOom() {
        return Observable.just(Success.Success).doOnCompleted(commandBuilder::debugOom);
    }

    @Override
    public Observable<String> debugReload() {
        return createObservable(() -> (commandBuilder.debugReload()));
    }

    @Override
    public Observable<String> debugRestart(Long delay) {
        return createObservable(() -> (commandBuilder.debugRestart(delay)));
    }

    @Override
    public Observable<String> debugSdslen(K key) {
        return createObservable(() -> (commandBuilder.debugSdslen(key)));
    }

    @Override
    public Observable<Success> debugSegfault() {
        return Observable.just(Success.Success).doOnCompleted(commandBuilder::debugSegfault);
    }

    @Override
    public Observable<Long> decr(K key) {
        return createObservable(() -> commandBuilder.decr(key));
    }

    @Override
    public Observable<Long> decrby(K key, long amount) {
        return createObservable(() -> commandBuilder.decrby(key, amount));
    }

    @Override
    public Observable<Long> del(K... keys) {
        return createObservable(() -> commandBuilder.del(keys));
    }

    public Observable<Long> del(Iterable<K> keys) {
        return createObservable(() -> commandBuilder.del(keys));
    }

    @Override
    public String digest(V script) {
        return LettuceStrings.digest(codec.encodeValue(script));
    }

    @Override
    public Observable<String> discard() {
        return createObservable(commandBuilder::discard);
    }

    @Override
    public <T> Observable<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return createDissolvingObservable(() -> new Command<>(type, output));
    }

    @Override
    public <T> Observable<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return createDissolvingObservable(() -> new Command<>(type, output, args));
    }

    @Override
    public Observable<byte[]> dump(K key) {
        return createObservable(() -> commandBuilder.dump(key));
    }

    @Override
    public Observable<V> echo(V msg) {
        return createObservable(() -> commandBuilder.echo(msg));
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

    @Override
    public Observable<Object> exec() {
        return createDissolvingObservable(EXEC, null, null);
    }

    public Observable<Boolean> exists(K key) {
        return createObservable(() -> commandBuilder.exists(key));
    }

    @Override
    public Observable<Long> exists(K... keys) {
        return createObservable(() -> commandBuilder.exists(keys));
    }

    public Observable<Long> exists(Iterable<K> keys) {
        return createObservable(() -> commandBuilder.exists(keys));
    }

    @Override
    public Observable<Boolean> expire(K key, long seconds) {
        return createObservable(() -> commandBuilder.expire(key, seconds));
    }

    @Override
    public Observable<Boolean> expireat(K key, long timestamp) {
        return createObservable(() -> commandBuilder.expireat(key, timestamp));
    }

    @Override
    public Observable<Boolean> expireat(K key, Date timestamp) {
        return expireat(key, timestamp.getTime() / 1000);
    }

    @Override
    public Observable<String> flushall() {
        return createObservable(commandBuilder::flushall);
    }

    @Override
    public Observable<String> flushallAsync() {
        return createObservable(commandBuilder::flushallAsync);
    }

    @Override
    public Observable<String> flushdb() {
        return createObservable(commandBuilder::flushdb);
    }

    @Override
    public Observable<String> flushdbAsync() {
        return createObservable(commandBuilder::flushdbAsync);
    }

    @Override
    public Observable<Long> geoadd(K key, double longitude, double latitude, V member) {
        return createObservable(() -> commandBuilder.geoadd(key, longitude, latitude, member));
    }

    @Override
    public Observable<Long> geoadd(K key, Object... lngLatMember) {
        return createDissolvingObservable(() -> commandBuilder.geoadd(key, lngLatMember));
    }

    @Override
    public Observable<Double> geodist(K key, V from, V to, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.geodist(key, from, to, unit));
    }

    @Override
    public Observable<String> geohash(K key, V... members) {
        return createDissolvingObservable(() -> commandBuilder.geohash(key, members));
    }

    @Override
    public Observable<GeoCoordinates> geopos(K key, V... members) {
        return createDissolvingObservable(() -> commandBuilder.geopos(key, members));
    }

    @Override
    public Observable<V> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance,
                unit.name()));
    }

    @Override
    public Observable<GeoWithin<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance,
                unit.name(), geoArgs));
    }

    @Override
    public Observable<Long> georadius(K key, double longitude, double latitude, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadius(key, longitude, latitude, distance, unit.name(),
                geoRadiusStoreArgs));
    }

    protected Observable<V> georadius_ro(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance,
                unit.name()));
    }

    protected Observable<GeoWithin<V>> georadius_ro(K key, double longitude, double latitude, double distance,
            GeoArgs.Unit unit, GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance,
                unit.name(), geoArgs));
    }

    @Override
    public Observable<V> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance,
                unit.name()));
    }

    @Override
    public Observable<GeoWithin<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance,
                unit.name(), geoArgs));
    }

    @Override
    public Observable<Long> georadiusbymember(K key, V member, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(key, member, distance, unit.name(),
                geoRadiusStoreArgs));
    }

    protected Observable<V> georadiusbymember_ro(K key, V member, double distance, GeoArgs.Unit unit) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance,
                unit.name()));
    }

    protected Observable<GeoWithin<V>> georadiusbymember_ro(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return createDissolvingObservable(() -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance,
                unit.name(), geoArgs));
    }

    @Override
    public Observable<V> get(K key) {
        return createObservable(() -> commandBuilder.get(key));
    }

    public StatefulConnection<K, V> getConnection() {
        return connection;
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

    @Override
    public Observable<Long> getbit(K key, long offset) {
        return createObservable(() -> commandBuilder.getbit(key, offset));
    }

    @Override
    public Observable<V> getrange(K key, long start, long end) {
        return createObservable(() -> commandBuilder.getrange(key, start, end));
    }

    @Override
    public Observable<V> getset(K key, V value) {
        return createObservable(() -> commandBuilder.getset(key, value));
    }

    @Override
    public Observable<Long> hdel(K key, K... fields) {
        return createObservable(() -> commandBuilder.hdel(key, fields));
    }

    @Override
    public Observable<Boolean> hexists(K key, K field) {
        return createObservable(() -> commandBuilder.hexists(key, field));
    }

    @Override
    public Observable<V> hget(K key, K field) {
        return createObservable(() -> commandBuilder.hget(key, field));
    }

    @Override
    public Observable<Map<K, V>> hgetall(K key) {
        return createObservable(() -> commandBuilder.hgetall(key));
    }

    @Override
    public Observable<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        return createObservable(() -> commandBuilder.hgetall(channel, key));
    }

    @Override
    public Observable<Long> hincrby(K key, K field, long amount) {
        return createObservable(() -> commandBuilder.hincrby(key, field, amount));
    }

    @Override
    public Observable<Double> hincrbyfloat(K key, K field, double amount) {
        return createObservable(() -> commandBuilder.hincrbyfloat(key, field, amount));
    }

    @Override
    public Observable<K> hkeys(K key) {
        return createDissolvingObservable(() -> commandBuilder.hkeys(key));
    }

    @Override
    public Observable<Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        return createObservable(() -> commandBuilder.hkeys(channel, key));
    }

    @Override
    public Observable<Long> hlen(K key) {
        return createObservable(() -> commandBuilder.hlen(key));
    }

    @Override
    public Observable<V> hmget(K key, K... fields) {
        return createDissolvingObservable(() -> commandBuilder.hmget(key, fields));
    }

    @Override
    public Observable<Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields) {
        return createObservable(() -> commandBuilder.hmget(channel, key, fields));
    }

    @Override
    public Observable<String> hmset(K key, Map<K, V> map) {
        return createObservable(() -> commandBuilder.hmset(key, map));
    }

    @Override
    public Observable<MapScanCursor<K, V>> hscan(K key) {
        return createObservable(() -> commandBuilder.hscan(key));
    }

    @Override
    public Observable<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.hscan(key, scanArgs));
    }

    @Override
    public Observable<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.hscan(key, scanCursor, scanArgs));
    }

    @Override
    public Observable<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.hscan(key, scanCursor));
    }

    @Override
    public Observable<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return createObservable(() -> commandBuilder.hscanStreaming(channel, key));
    }

    @Override
    public Observable<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.hscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.hscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.hscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Observable<Boolean> hset(K key, K field, V value) {
        return createObservable(() -> commandBuilder.hset(key, field, value));
    }

    @Override
    public Observable<Boolean> hsetnx(K key, K field, V value) {
        return createObservable(() -> commandBuilder.hsetnx(key, field, value));
    }

    @Override
    public Observable<Long> hstrlen(K key, K field) {
        return createObservable(() -> commandBuilder.hstrlen(key, field));
    }

    @Override
    public Observable<V> hvals(K key) {
        return createDissolvingObservable(() -> commandBuilder.hvals(key));
    }

    @Override
    public Observable<Long> hvals(ValueStreamingChannel<V> channel, K key) {
        return createObservable(() -> commandBuilder.hvals(channel, key));
    }

    @Override
    public Observable<Long> incr(K key) {
        return createObservable(() -> commandBuilder.incr(key));
    }

    @Override
    public Observable<Long> incrby(K key, long amount) {
        return createObservable(() -> commandBuilder.incrby(key, amount));
    }

    @Override
    public Observable<Double> incrbyfloat(K key, double amount) {
        return createObservable(() -> commandBuilder.incrbyfloat(key, amount));
    }

    @Override
    public Observable<String> info() {
        return createObservable(commandBuilder::info);
    }

    @Override
    public Observable<String> info(String section) {
        return createObservable(() -> commandBuilder.info(section));
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public Observable<K> keys(K pattern) {
        return createDissolvingObservable(() -> commandBuilder.keys(pattern));
    }

    @Override
    public Observable<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        return createObservable(() -> commandBuilder.keys(channel, pattern));
    }

    @Override
    public Observable<Date> lastsave() {
        return createObservable(commandBuilder::lastsave);
    }

    @Override
    public Observable<V> lindex(K key, long index) {
        return createObservable(() -> commandBuilder.lindex(key, index));
    }

    @Override
    public Observable<Long> linsert(K key, boolean before, V pivot, V value) {
        return createObservable(() -> commandBuilder.linsert(key, before, pivot, value));
    }

    @Override
    public Observable<Long> llen(K key) {
        return createObservable(() -> commandBuilder.llen(key));
    }

    @Override
    public Observable<V> lpop(K key) {
        return createObservable(() -> commandBuilder.lpop(key));
    }

    @Override
    public Observable<Long> lpush(K key, V... values) {
        return createObservable(() -> commandBuilder.lpush(key, values));
    }

    @Override
    public Observable<Long> lpushx(K key, V value) {
        return createObservable(() -> commandBuilder.lpushx(key, value));
    }

    @Override
    public Observable<Long> lpushx(K key, V... values) {
        return createObservable(() -> commandBuilder.lpushx(key, values));
    }

    @Override
    public Observable<V> lrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.lrange(key, start, stop));
    }

    @Override
    public Observable<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createObservable(() -> commandBuilder.lrange(channel, key, start, stop));
    }

    @Override
    public Observable<Long> lrem(K key, long count, V value) {
        return createObservable(() -> commandBuilder.lrem(key, count, value));
    }

    @Override
    public Observable<String> lset(K key, long index, V value) {
        return createObservable(() -> commandBuilder.lset(key, index, value));
    }

    @Override
    public Observable<String> ltrim(K key, long start, long stop) {
        return createObservable(() -> commandBuilder.ltrim(key, start, stop));
    }

    @Override
    public Observable<V> mget(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.mget(keys));
    }

    public Observable<V> mget(Iterable<K> keys) {
        return createDissolvingObservable(() -> commandBuilder.mget(keys));
    }

    @Override
    public Observable<Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        return createObservable(() -> commandBuilder.mget(channel, keys));
    }

    public Observable<Long> mget(ValueStreamingChannel<V> channel, Iterable<K> keys) {
        return createObservable(() -> commandBuilder.mget(channel, keys));
    }

    @Override
    public Observable<String> migrate(String host, int port, K key, int db, long timeout) {
        return createObservable(() -> commandBuilder.migrate(host, port, key, db, timeout));
    }

    @Override
    public Observable<String> migrate(String host, int port, int db, long timeout, MigrateArgs<K> migrateArgs) {
        return createObservable(() -> commandBuilder.migrate(host, port, db, timeout, migrateArgs));
    }

    @Override
    public Observable<Boolean> move(K key, int db) {
        return createObservable(() -> commandBuilder.move(key, db));
    }

    @Override
    public Observable<String> mset(Map<K, V> map) {
        return createObservable(() -> commandBuilder.mset(map));
    }

    @Override
    public Observable<Boolean> msetnx(Map<K, V> map) {
        return createObservable(() -> commandBuilder.msetnx(map));
    }

    @Override
    public Observable<String> multi() {
        return createObservable(commandBuilder::multi);
    }

    @Override
    public Observable<String> objectEncoding(K key) {
        return createObservable(() -> commandBuilder.objectEncoding(key));
    }

    @Override
    public Observable<Long> objectIdletime(K key) {
        return createObservable(() -> commandBuilder.objectIdletime(key));
    }

    @Override
    public Observable<Long> objectRefcount(K key) {
        return createObservable(() -> commandBuilder.objectRefcount(key));
    }

    @Override
    public Observable<Boolean> persist(K key) {
        return createObservable(() -> commandBuilder.persist(key));
    }

    @Override
    public Observable<Boolean> pexpire(K key, long milliseconds) {
        return createObservable(() -> commandBuilder.pexpire(key, milliseconds));
    }

    @Override
    public Observable<Boolean> pexpireat(K key, Date timestamp) {
        return pexpireat(key, timestamp.getTime());
    }

    @Override
    public Observable<Boolean> pexpireat(K key, long timestamp) {
        return createObservable(() -> commandBuilder.pexpireat(key, timestamp));
    }

    @Override
    public Observable<Long> pfadd(K key, V... values) {
        return createObservable(() -> commandBuilder.pfadd(key, values));
    }

    public Observable<Long> pfadd(K key, V value, V... values) {
        return createObservable(() -> commandBuilder.pfadd(key, value, values));
    }

    @Override
    public Observable<Long> pfcount(K... keys) {
        return createObservable(() -> commandBuilder.pfcount(keys));
    }

    public Observable<Long> pfcount(K key, K... keys) {
        return createObservable(() -> commandBuilder.pfcount(key, keys));
    }

    @Override
    public Observable<String> pfmerge(K destkey, K... sourcekeys) {
        return createObservable(() -> commandBuilder.pfmerge(destkey, sourcekeys));
    }

    public Observable<String> pfmerge(K destkey, K sourceKey, K... sourcekeys) {
        return createObservable(() -> commandBuilder.pfmerge(destkey, sourceKey, sourcekeys));
    }

    @Override
    public Observable<String> ping() {
        return createObservable(commandBuilder::ping);
    }

    @Override
    public Observable<String> psetex(K key, long milliseconds, V value) {
        return createObservable(() -> commandBuilder.psetex(key, milliseconds, value));
    }

    @Override
    public Observable<Long> pttl(K key) {
        return createObservable(() -> commandBuilder.pttl(key));
    }

    @Override
    public Observable<Long> publish(K channel, V message) {
        return createObservable(() -> commandBuilder.publish(channel, message));
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
    public Observable<Long> pubsubNumpat() {
        return createObservable(commandBuilder::pubsubNumpat);
    }

    @Override
    public Observable<Map<K, Long>> pubsubNumsub(K... channels) {
        return createObservable(() -> commandBuilder.pubsubNumsub(channels));
    }

    @Override
    public Observable<String> quit() {
        return createObservable(commandBuilder::quit);
    }

    @Override
    public Observable<V> randomkey() {
        return createObservable(commandBuilder::randomkey);
    }

    @Override
    public Observable<String> readOnly() {
        return createObservable(commandBuilder::readOnly);
    }

    @Override
    public Observable<String> readWrite() {
        return createObservable(commandBuilder::readWrite);
    }

    @Override
    public Observable<String> rename(K key, K newKey) {
        return createObservable(() -> commandBuilder.rename(key, newKey));
    }

    @Override
    public Observable<Boolean> renamenx(K key, K newKey) {
        return createObservable(() -> commandBuilder.renamenx(key, newKey));
    }

    @Override
    public void reset() {
        getConnection().reset();
    }

    @Override
    public Observable<String> restore(K key, long ttl, byte[] value) {
        return createObservable(() -> commandBuilder.restore(key, ttl, value));
    }

    @Override
    public Observable<Object> role() {
        return createDissolvingObservable(commandBuilder::role);
    }

    @Override
    public Observable<V> rpop(K key) {
        return createObservable(() -> commandBuilder.rpop(key));
    }

    @Override
    public Observable<V> rpoplpush(K source, K destination) {
        return createObservable(() -> commandBuilder.rpoplpush(source, destination));
    }

    @Override
    public Observable<Long> rpush(K key, V... values) {
        return createObservable(() -> commandBuilder.rpush(key, values));
    }

    @Override
    public Observable<Long> rpushx(K key, V value) {
        return createObservable(() -> commandBuilder.rpushx(key, value));
    }

    @Override
    public Observable<Long> rpushx(K key, V... values) {
        return createObservable(() -> commandBuilder.rpushx(key, values));
    }

    @Override
    public Observable<Long> sadd(K key, V... members) {
        return createObservable(() -> commandBuilder.sadd(key, members));
    }

    @Override
    public Observable<String> save() {
        return createObservable(commandBuilder::save);
    }

    @Override
    public Observable<KeyScanCursor<K>> scan() {
        return createObservable(commandBuilder::scan);
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.scan(scanArgs));
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.scan(scanCursor, scanArgs));
    }

    @Override
    public Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.scan(scanCursor));
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return createObservable(() -> commandBuilder.scanStreaming(channel));
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.scanStreaming(channel, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.scanStreaming(channel, scanCursor, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.scanStreaming(channel, scanCursor));
    }

    @Override
    public Observable<Long> scard(K key) {
        return createObservable(() -> commandBuilder.scard(key));
    }

    @Override
    public Observable<Boolean> scriptExists(String... digests) {
        return createDissolvingObservable(() -> commandBuilder.scriptExists(digests));
    }

    @Override
    public Observable<String> scriptFlush() {
        return createObservable(commandBuilder::scriptFlush);
    }

    @Override
    public Observable<String> scriptKill() {
        return createObservable(commandBuilder::scriptKill);
    }

    @Override
    public Observable<String> scriptLoad(V script) {
        return createObservable(() -> commandBuilder.scriptLoad(script));
    }

    @Override
    public Observable<V> sdiff(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sdiff(keys));
    }

    @Override
    public Observable<Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        return createObservable(() -> commandBuilder.sdiff(channel, keys));
    }

    @Override
    public Observable<Long> sdiffstore(K destination, K... keys) {
        return createObservable(() -> commandBuilder.sdiffstore(destination, keys));
    }

    public Observable<String> select(int db) {
        return createObservable(() -> commandBuilder.select(db));
    }

    @Override
    public Observable<String> set(K key, V value) {
        return createObservable(() -> commandBuilder.set(key, value));
    }

    @Override
    public Observable<String> set(K key, V value, SetArgs setArgs) {
        return createObservable(() -> commandBuilder.set(key, value, setArgs));
    }

    @Override
    public void setTimeout(long timeout, TimeUnit unit) {
        connection.setTimeout(timeout, unit);
    }

    @Override
    public Observable<Long> setbit(K key, long offset, int value) {
        return createObservable(() -> commandBuilder.setbit(key, offset, value));
    }

    @Override
    public Observable<String> setex(K key, long seconds, V value) {
        return createObservable(() -> commandBuilder.setex(key, seconds, value));
    }

    @Override
    public Observable<Boolean> setnx(K key, V value) {
        return createObservable(() -> commandBuilder.setnx(key, value));
    }

    @Override
    public Observable<Long> setrange(K key, long offset, V value) {
        return createObservable(() -> commandBuilder.setrange(key, offset, value));
    }

    @Override
    public Observable<Success> shutdown(boolean save) {
        return getSuccessObservable(createObservable(() -> commandBuilder.shutdown(save)));
    }

    @Override
    public Observable<V> sinter(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sinter(keys));
    }

    @Override
    public Observable<Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        return createObservable(() -> commandBuilder.sinter(channel, keys));
    }

    @Override
    public Observable<Long> sinterstore(K destination, K... keys) {
        return createObservable(() -> commandBuilder.sinterstore(destination, keys));
    }

    @Override
    public Observable<Boolean> sismember(K key, V member) {
        return createObservable(() -> commandBuilder.sismember(key, member));
    }

    @Override
    public Observable<String> slaveof(String host, int port) {
        return createObservable(() -> commandBuilder.slaveof(host, port));
    }

    @Override
    public Observable<String> slaveofNoOne() {
        return createObservable(() -> commandBuilder.slaveofNoOne());
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
    public Observable<Long> slowlogLen() {
        return createObservable(() -> commandBuilder.slowlogLen());
    }

    @Override
    public Observable<String> slowlogReset() {
        return createObservable(() -> commandBuilder.slowlogReset());
    }

    @Override
    public Observable<V> smembers(K key) {
        return createDissolvingObservable(() -> commandBuilder.smembers(key));
    }

    @Override
    public Observable<Long> smembers(ValueStreamingChannel<V> channel, K key) {
        return createObservable(() -> commandBuilder.smembers(channel, key));
    }

    @Override
    public Observable<Boolean> smove(K source, K destination, V member) {
        return createObservable(() -> commandBuilder.smove(source, destination, member));
    }

    @Override
    public Observable<V> sort(K key) {
        return createDissolvingObservable(() -> commandBuilder.sort(key));
    }

    @Override
    public Observable<Long> sort(ValueStreamingChannel<V> channel, K key) {
        return createObservable(() -> commandBuilder.sort(channel, key));
    }

    @Override
    public Observable<V> sort(K key, SortArgs sortArgs) {
        return createDissolvingObservable(() -> commandBuilder.sort(key, sortArgs));
    }

    @Override
    public Observable<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        return createObservable(() -> commandBuilder.sort(channel, key, sortArgs));
    }

    @Override
    public Observable<Long> sortStore(K key, SortArgs sortArgs, K destination) {
        return createObservable(() -> commandBuilder.sortStore(key, sortArgs, destination));
    }

    @Override
    public Observable<V> spop(K key) {
        return createObservable(() -> commandBuilder.spop(key));
    }

    @Override
    public Observable<V> spop(K key, long count) {
        return createDissolvingObservable(() -> commandBuilder.spop(key, count));
    }

    @Override
    public Observable<V> srandmember(K key) {
        return createObservable(() -> commandBuilder.srandmember(key));
    }

    @Override
    public Observable<V> srandmember(K key, long count) {
        return createDissolvingObservable(() -> commandBuilder.srandmember(key, count));
    }

    @Override
    public Observable<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        return createObservable(() -> commandBuilder.srandmember(channel, key, count));
    }

    @Override
    public Observable<Long> srem(K key, V... members) {
        return createObservable(() -> commandBuilder.srem(key, members));
    }

    @Override
    public Observable<ValueScanCursor<V>> sscan(K key) {
        return createObservable(() -> commandBuilder.sscan(key));
    }

    @Override
    public Observable<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.sscan(key, scanArgs));
    }

    @Override
    public Observable<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.sscan(key, scanCursor, scanArgs));
    }

    @Override
    public Observable<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.sscan(key, scanCursor));
    }

    @Override
    public Observable<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return createObservable(() -> commandBuilder.sscanStreaming(channel, key));
    }

    @Override
    public Observable<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.sscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.sscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.sscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Observable<Long> strlen(K key) {
        return createObservable(() -> commandBuilder.strlen(key));
    }

    @Override
    public Observable<V> sunion(K... keys) {
        return createDissolvingObservable(() -> commandBuilder.sunion(keys));
    }

    @Override
    public Observable<Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        return createObservable(() -> commandBuilder.sunion(channel, keys));
    }

    @Override
    public Observable<Long> sunionstore(K destination, K... keys) {
        return createObservable(() -> commandBuilder.sunionstore(destination, keys));
    }

    public Observable<String> swapdb(int db1, int db2) {
        return createObservable(() -> commandBuilder.swapdb(db1, db2));
    }

    @Override
    public Observable<String> sync() {
        return createObservable(commandBuilder::sync);
    }

    @Override
    public Observable<V> time() {
        return createDissolvingObservable(commandBuilder::time);
    }

    @Override
    public Observable<Long> touch(K... keys) {
        return createObservable(() -> commandBuilder.touch(keys));
    }

    public Observable<Long> touch(Iterable<K> keys) {
        return createObservable(() -> commandBuilder.touch(keys));
    }

    @Override
    public Observable<Long> ttl(K key) {
        return createObservable(() -> commandBuilder.ttl(key));
    }

    @Override
    public Observable<String> type(K key) {
        return createObservable(() -> commandBuilder.type(key));
    }

    @Override
    public Observable<Long> unlink(K... keys) {
        return createObservable(() -> commandBuilder.unlink(keys));
    }

    public Observable<Long> unlink(Iterable<K> keys) {
        return createObservable(() -> commandBuilder.unlink(keys));
    }

    @Override
    public Observable<String> unwatch() {
        return createObservable(commandBuilder::unwatch);
    }

    @Override
    public Observable<Long> waitForReplication(int replicas, long timeout) {
        return createObservable(() -> commandBuilder.wait(replicas, timeout));
    }

    @Override
    public Observable<String> watch(K... keys) {
        return createObservable(() -> commandBuilder.watch(keys));
    }

    @Override
    public Observable<String> xadd(K key, Map<K, V> body) {
        return createObservable(() -> commandBuilder.xadd(key, null, body));
    }

    @Override
    public Observable<String> xadd(K key, XAddArgs args, Map<K, V> body) {
        return createObservable(() -> commandBuilder.xadd(key, args, body));
    }

    @Override
    public Observable<String> xadd(K key, Object... keysAndValues) {
        return createObservable(() -> commandBuilder.xadd(key, null, keysAndValues));
    }

    @Override
    public Observable<String> xadd(K key, XAddArgs args, Object... keysAndValues) {
        return createObservable(() -> commandBuilder.xadd(key, args, keysAndValues));
    }

    @Override
    public Observable<Long> xlen(K key) {
        return createObservable(() -> commandBuilder.xlen(key));
    }

    @Override
    public Observable<StreamMessage<K, V>> xrange(K key, Range<String> range) {
        return createDissolvingObservable(() -> commandBuilder.xrange(key, range, Limit.unlimited()));
    }

    @Override
    public Observable<StreamMessage<K, V>> xrange(K key, Range<String> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.xrange(key, range, limit));
    }

    @Override
    public Observable<StreamMessage<K, V>> xrevrange(K key, Range<String> range) {
        return createDissolvingObservable(() -> commandBuilder.xrevrange(key, range, Limit.unlimited()));
    }

    @Override
    public Observable<StreamMessage<K, V>> xrevrange(K key, Range<String> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.xrevrange(key, range, limit));
    }

    @Override
    public Observable<StreamMessage<K, V>> xread(XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingObservable(() -> commandBuilder.xread(streams, null));
    }

    @Override
    public Observable<StreamMessage<K, V>> xread(XReadArgs args, XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingObservable(() -> commandBuilder.xread(streams, args));
    }

    @Override
    public Observable<Long> xack(K key, String group, String... messageIds) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xclaim(K key, Consumer consumer, XClaimArgs args, String... messageIds) {
        return null;
    }

    @Override
    public Observable<Boolean> xgroupDelconsumer(K key, Consumer consumer) {
        return null;
    }

    @Override
    public Observable<Boolean> xgroupCreate(K key, String group, String offset) {
        return null;
    }

    @Override
    public Observable<Boolean> xgroupSetid(K key, String group, String offset) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xpending(K key, String group) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xpending(K key, Consumer consumer) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xpending(K key, String group, Range<String> range, Limit limit) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xpending(K key, Consumer consumer, Range<String> range, Limit limit) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xreadgroup(Consumer consumer, XReadArgs.StreamOffset<K>... streams) {
        return null;
    }

    @Override
    public Observable<StreamMessage<K, V>> xreadgroup(Consumer consumer, XReadArgs args, XReadArgs.StreamOffset<K>... streams) {
        return null;
    }

    @Override
    public Observable<Long> zadd(K key, double score, V member) {
        return createObservable(() -> commandBuilder.zadd(key, null, score, member));
    }

    @Override
    public Observable<Long> zadd(K key, Object... scoresAndValues) {
        return createObservable(() -> commandBuilder.zadd(key, null, scoresAndValues));
    }

    @Override
    public Observable<Long> zadd(K key, ScoredValue<V>... scoredValues) {
        return createObservable(() -> commandBuilder.zadd(key, null, scoredValues));
    }

    @Override
    public Observable<Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return createObservable(() -> commandBuilder.zadd(key, zAddArgs, score, member));
    }

    @Override
    public Observable<Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        return createObservable(() -> commandBuilder.zadd(key, zAddArgs, scoresAndValues));
    }

    @Override
    public Observable<Long> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... scoredValues) {
        return createObservable(() -> commandBuilder.zadd(key, zAddArgs, scoredValues));
    }

    @Override
    public Observable<Double> zaddincr(K key, double score, V member) {
        return createObservable(() -> commandBuilder.zaddincr(key, null, score, member));
    }

    @Override
    public Observable<Double> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        return createObservable(() -> commandBuilder.zaddincr(key, zAddArgs, score, member));
    }

    @Override
    public Observable<Long> zcard(K key) {
        return createObservable(() -> commandBuilder.zcard(key));
    }

    @Override
    public Observable<Long> zcount(K key, double min, double max) {
        return createObservable(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Observable<Long> zcount(K key, String min, String max) {
        return createObservable(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Observable<Long> zcount(K key, Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zcount(key, range));
    }

    @Override
    public Observable<Double> zincrby(K key, double amount, K member) {
        return createObservable(() -> commandBuilder.zincrby(key, amount, member));
    }

    @Override
    public Observable<Long> zinterstore(K destination, K... keys) {
        return createObservable(() -> commandBuilder.zinterstore(destination, keys));
    }

    @Override
    public Observable<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createObservable(() -> commandBuilder.zinterstore(destination, storeArgs, keys));
    }

    @Override
    public Observable<Long> zlexcount(K key, String min, String max) {
        return createObservable(() -> commandBuilder.zlexcount(key, min, max));
    }

    @Override
    public Observable<Long> zlexcount(K key, Range<? extends V> range) {
        return createObservable(() -> commandBuilder.zlexcount(key, range));
    }

    @Override
    public Observable<V> zrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrange(key, start, stop));
    }

    @Override
    public Observable<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createObservable(() -> commandBuilder.zrange(channel, key, start, stop));
    }

    @Override
    public Observable<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrangeWithScores(key, start, stop));
    }

    @Override
    public Observable<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createObservable(() -> commandBuilder.zrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Observable<V> zrangebylex(K key, String min, String max) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, min, max));
    }

    @Override
    public Observable<V> zrangebylex(K key, Range<? extends V> range) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public Observable<V> zrangebylex(K key, String min, String max, long offset, long count) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, min, max, offset, count));
    }

    @Override
    public Observable<V> zrangebylex(K key, Range<? extends V> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrangebylex(key, range, limit));
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
    public Observable<V> zrangebyscore(K key, Range<? extends Number> range) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, range, Limit.unlimited()));
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
    public Observable<V> zrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscore(key, range, limit));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset,
            long count) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset,
            long count) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Observable<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
        return createObservable(() -> commandBuilder.zrangebyscore(channel, key, range, limit));
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
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, range, Limit.unlimited()));
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
    public Observable<ScoredValue<V>> zrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Observable<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        return createObservable(() -> commandBuilder.zrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public Observable<Long> zrank(K key, V member) {
        return createObservable(() -> commandBuilder.zrank(key, member));
    }

    @Override
    public Observable<Long> zrem(K key, V... members) {
        return createObservable(() -> commandBuilder.zrem(key, members));
    }

    @Override
    public Observable<Long> zremrangebylex(K key, String min, String max) {
        return createObservable(() -> commandBuilder.zremrangebylex(key, min, max));
    }

    @Override
    public Observable<Long> zremrangebylex(K key, Range<? extends V> range) {
        return createObservable(() -> commandBuilder.zremrangebylex(key, range));
    }

    @Override
    public Observable<Long> zremrangebyrank(K key, long start, long stop) {
        return createObservable(() -> commandBuilder.zremrangebyrank(key, start, stop));
    }

    @Override
    public Observable<Long> zremrangebyscore(K key, double min, double max) {
        return createObservable(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Observable<Long> zremrangebyscore(K key, String min, String max) {
        return createObservable(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Observable<Long> zremrangebyscore(K key, Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zremrangebyscore(key, range));
    }

    @Override
    public Observable<V> zrevrange(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrevrange(key, start, stop));
    }

    @Override
    public Observable<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createObservable(() -> commandBuilder.zrevrange(channel, key, start, stop));
    }

    @Override
    public Observable<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangeWithScores(key, start, stop));
    }

    @Override
    public Observable<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createObservable(() -> commandBuilder.zrevrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Observable<V> zrevrangebylex(K key, Range<? extends V> range) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public Observable<V> zrevrangebylex(K key, Range<? extends V> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebylex(key, range, limit));
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
    public Observable<V> zrevrangebyscore(K key, Range<? extends Number> range) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, range, Limit.unlimited()));
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
    public Observable<V> zrevrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscore(key, range, limit));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Observable<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
        return createObservable(() -> commandBuilder.zrevrangebyscore(channel, key, range, limit));
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
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, range, Limit.unlimited()));
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
    public Observable<ScoredValue<V>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingObservable(() -> commandBuilder.zrevrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Observable<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        return createObservable(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public Observable<Long> zrevrank(K key, V member) {
        return createObservable(() -> commandBuilder.zrevrank(key, member));
    }

    @Override
    public Observable<ScoredValueScanCursor<V>> zscan(K key) {
        return createObservable(() -> commandBuilder.zscan(key));
    }

    @Override
    public Observable<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.zscan(key, scanArgs));
    }

    @Override
    public Observable<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.zscan(key, scanCursor, scanArgs));
    }

    @Override
    public Observable<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.zscan(key, scanCursor));
    }

    @Override
    public Observable<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return createObservable(() -> commandBuilder.zscanStreaming(channel, key));
    }

    @Override
    public Observable<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.zscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createObservable(() -> commandBuilder.zscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Observable<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createObservable(() -> commandBuilder.zscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Observable<Double> zscore(K key, V member) {
        return createObservable(() -> commandBuilder.zscore(key, member));
    }

    @Override
    public Observable<Long> zunionstore(K destination, K... keys) {
        return createObservable(() -> commandBuilder.zunionstore(destination, keys));
    }

    @Override
    public Observable<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createObservable(() -> commandBuilder.zunionstore(destination, storeArgs, keys));
    }
}
