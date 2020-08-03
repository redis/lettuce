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
import java.util.Map;
import java.util.function.Supplier;

import io.lettuce.core.codec.Base16;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.stream.PendingMessage;
import io.lettuce.core.models.stream.PendingMessages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.*;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.Tracing;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * A reactive and thread-safe API for a Redis connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Nikolai Perevozchikov
 * @author Tugdual Grall
 * @since 4.0
 */
public abstract class AbstractRedisReactiveCommands<K, V> implements RedisHashReactiveCommands<K, V>,
        RedisKeyReactiveCommands<K, V>, RedisStringReactiveCommands<K, V>, RedisListReactiveCommands<K, V>,
        RedisSetReactiveCommands<K, V>, RedisSortedSetReactiveCommands<K, V>, RedisScriptingReactiveCommands<K, V>,
        RedisServerReactiveCommands<K, V>, RedisHLLReactiveCommands<K, V>, BaseRedisReactiveCommands<K, V>,
        RedisTransactionalReactiveCommands<K, V>, RedisGeoReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    private final Object mutex = new Object();

    private final StatefulConnection<K, V> connection;

    private final RedisCommandBuilder<K, V> commandBuilder;

    private final ClientResources clientResources;

    private final boolean tracingEnabled;

    private EventExecutorGroup scheduler;

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     */
    public AbstractRedisReactiveCommands(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        this.commandBuilder = new RedisCommandBuilder<>(codec);
        this.clientResources = connection.getResources();
        this.tracingEnabled = clientResources.tracing().isEnabled();
    }

    private EventExecutorGroup getScheduler() {

        if (this.scheduler != null) {
            return this.scheduler;
        }

        synchronized (mutex) {

            EventExecutorGroup scheduler = ImmediateEventExecutor.INSTANCE;

            if (connection.getOptions().isPublishOnScheduler()) {
                scheduler = connection.getResources().eventExecutorGroup();
            }

            return this.scheduler = scheduler;
        }
    }

    @Override
    public Mono<Long> append(K key, V value) {
        return createMono(() -> commandBuilder.append(key, value));
    }

    @Override
    public Mono<String> asking() {
        return createMono(commandBuilder::asking);
    }

    @Override
    public Mono<String> auth(CharSequence password) {
        return createMono(() -> commandBuilder.auth(password));
    }

    @Override
    public Mono<String> auth(String username, CharSequence password) {
        return createMono(() -> commandBuilder.auth(username, password));
    }

    @Override
    public Mono<String> bgrewriteaof() {
        return createMono(commandBuilder::bgrewriteaof);
    }

    @Override
    public Mono<String> bgsave() {
        return createMono(commandBuilder::bgsave);
    }

    @Override
    public Mono<Long> bitcount(K key) {
        return createMono(() -> commandBuilder.bitcount(key));
    }

    @Override
    public Mono<Long> bitcount(K key, long start, long end) {
        return createMono(() -> commandBuilder.bitcount(key, start, end));
    }

    @Override
    public Flux<Value<Long>> bitfield(K key, BitFieldArgs args) {
        return createDissolvingFlux(() -> commandBuilder.bitfieldValue(key, args));
    }

    @Override
    public Mono<Long> bitopAnd(K destination, K... keys) {
        return createMono(() -> commandBuilder.bitopAnd(destination, keys));
    }

    @Override
    public Mono<Long> bitopNot(K destination, K source) {
        return createMono(() -> commandBuilder.bitopNot(destination, source));
    }

    @Override
    public Mono<Long> bitopOr(K destination, K... keys) {
        return createMono(() -> commandBuilder.bitopOr(destination, keys));
    }

    @Override
    public Mono<Long> bitopXor(K destination, K... keys) {
        return createMono(() -> commandBuilder.bitopXor(destination, keys));
    }

    @Override
    public Mono<Long> bitpos(K key, boolean state) {
        return createMono(() -> commandBuilder.bitpos(key, state));
    }

    @Override
    public Mono<Long> bitpos(K key, boolean state, long start) {
        return createMono(() -> commandBuilder.bitpos(key, state, start));
    }

    @Override
    public Mono<Long> bitpos(K key, boolean state, long start, long end) {
        return createMono(() -> commandBuilder.bitpos(key, state, start, end));
    }

    @Override
    public Mono<KeyValue<K, V>> blpop(long timeout, K... keys) {
        return createMono(() -> commandBuilder.blpop(timeout, keys));
    }

    @Override
    public Mono<KeyValue<K, V>> brpop(long timeout, K... keys) {
        return createMono(() -> commandBuilder.brpop(timeout, keys));
    }

    @Override
    public Mono<V> brpoplpush(long timeout, K source, K destination) {
        return createMono(() -> commandBuilder.brpoplpush(timeout, source, destination));
    }

    @Override
    public Mono<String> clientCaching(boolean enabled) {
        return createMono(() -> commandBuilder.clientCaching(enabled));
    }

    @Override
    public Mono<K> clientGetname() {
        return createMono(commandBuilder::clientGetname);
    }

    @Override
    public Mono<Long> clientGetredir() {
        return createMono(commandBuilder::clientGetredir);
    }

    @Override
    public Mono<String> clientKill(String addr) {
        return createMono(() -> commandBuilder.clientKill(addr));
    }

    @Override
    public Mono<Long> clientKill(KillArgs killArgs) {
        return createMono(() -> commandBuilder.clientKill(killArgs));
    }

    @Override
    public Mono<String> clientList() {
        return createMono(commandBuilder::clientList);
    }

    @Override
    public Mono<Long> clientId() {
        return createMono(commandBuilder::clientId);
    }

    @Override
    public Mono<String> clientPause(long timeout) {
        return createMono(() -> commandBuilder.clientPause(timeout));
    }

    @Override
    public Mono<String> clientSetname(K name) {
        return createMono(() -> commandBuilder.clientSetname(name));
    }

    @Override
    public Mono<String> clientTracking(TrackingArgs args) {
        return createMono(() -> commandBuilder.clientTracking(args));
    }

    @Override
    public Mono<Long> clientUnblock(long id, UnblockType type) {
        return createMono(() -> commandBuilder.clientUnblock(id, type));
    }

    public void close() {
        connection.close();
    }

    @Override
    public Mono<String> clusterAddSlots(int... slots) {
        return createMono(() -> commandBuilder.clusterAddslots(slots));
    }

    @Override
    public Mono<String> clusterBumpepoch() {
        return createMono(() -> commandBuilder.clusterBumpepoch());
    }

    @Override
    public Mono<Long> clusterCountFailureReports(String nodeId) {
        return createMono(() -> commandBuilder.clusterCountFailureReports(nodeId));
    }

    @Override
    public Mono<Long> clusterCountKeysInSlot(int slot) {
        return createMono(() -> commandBuilder.clusterCountKeysInSlot(slot));
    }

    @Override
    public Mono<String> clusterDelSlots(int... slots) {
        return createMono(() -> commandBuilder.clusterDelslots(slots));
    }

    @Override
    public Mono<String> clusterFailover(boolean force) {
        return createMono(() -> commandBuilder.clusterFailover(force));
    }

    @Override
    public Mono<String> clusterFlushslots() {
        return createMono(commandBuilder::clusterFlushslots);
    }

    @Override
    public Mono<String> clusterForget(String nodeId) {
        return createMono(() -> commandBuilder.clusterForget(nodeId));
    }

    @Override
    public Flux<K> clusterGetKeysInSlot(int slot, int count) {
        return createDissolvingFlux(() -> commandBuilder.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public Mono<String> clusterInfo() {
        return createMono(commandBuilder::clusterInfo);
    }

    @Override
    public Mono<Long> clusterKeyslot(K key) {
        return createMono(() -> commandBuilder.clusterKeyslot(key));
    }

    @Override
    public Mono<String> clusterMeet(String ip, int port) {
        return createMono(() -> commandBuilder.clusterMeet(ip, port));
    }

    @Override
    public Mono<String> clusterMyId() {
        return createMono(commandBuilder::clusterMyId);
    }

    @Override
    public Mono<String> clusterNodes() {
        return createMono(commandBuilder::clusterNodes);
    }

    @Override
    public Mono<String> clusterReplicate(String nodeId) {
        return createMono(() -> commandBuilder.clusterReplicate(nodeId));
    }

    @Override
    public Mono<String> clusterReset(boolean hard) {
        return createMono(() -> commandBuilder.clusterReset(hard));
    }

    @Override
    public Mono<String> clusterSaveconfig() {
        return createMono(() -> commandBuilder.clusterSaveconfig());
    }

    @Override
    public Mono<String> clusterSetConfigEpoch(long configEpoch) {
        return createMono(() -> commandBuilder.clusterSetConfigEpoch(configEpoch));
    }

    @Override
    public Mono<String> clusterSetSlotImporting(int slot, String nodeId) {
        return createMono(() -> commandBuilder.clusterSetSlotImporting(slot, nodeId));
    }

    @Override
    public Mono<String> clusterSetSlotMigrating(int slot, String nodeId) {
        return createMono(() -> commandBuilder.clusterSetSlotMigrating(slot, nodeId));
    }

    @Override
    public Mono<String> clusterSetSlotNode(int slot, String nodeId) {
        return createMono(() -> commandBuilder.clusterSetSlotNode(slot, nodeId));
    }

    @Override
    public Mono<String> clusterSetSlotStable(int slot) {
        return createMono(() -> commandBuilder.clusterSetSlotStable(slot));
    }

    @Override
    public Flux<String> clusterSlaves(String nodeId) {
        return createDissolvingFlux(() -> commandBuilder.clusterSlaves(nodeId));
    }

    @Override
    public Flux<Object> clusterSlots() {
        return createDissolvingFlux(commandBuilder::clusterSlots);
    }

    @Override
    public Flux<Object> command() {
        return createDissolvingFlux(commandBuilder::command);
    }

    @Override
    public Mono<Long> commandCount() {
        return createMono(commandBuilder::commandCount);
    }

    @Override
    public Flux<Object> commandInfo(String... commands) {
        return createDissolvingFlux(() -> commandBuilder.commandInfo(commands));
    }

    @Override
    public Flux<Object> commandInfo(CommandType... commands) {
        String[] stringCommands = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            stringCommands[i] = commands[i].name();
        }

        return commandInfo(stringCommands);
    }

    @Override
    public Mono<Map<String, String>> configGet(String parameter) {
        return createMono(() -> commandBuilder.configGet(parameter));
    }

    @Override
    public Mono<String> configResetstat() {
        return createMono(commandBuilder::configResetstat);
    }

    @Override
    public Mono<String> configRewrite() {
        return createMono(commandBuilder::configRewrite);
    }

    @Override
    public Mono<String> configSet(String parameter, String value) {
        return createMono(() -> commandBuilder.configSet(parameter, value));
    }

    @SuppressWarnings("unchecked")
    public <T, R> Flux<R> createDissolvingFlux(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return (Flux<R>) createFlux(commandSupplier, true);
    }

    public <T> Flux<T> createFlux(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return createFlux(commandSupplier, false);
    }

    private <T> Flux<T> createFlux(Supplier<RedisCommand<K, V, T>> commandSupplier, boolean dissolve) {

        if (tracingEnabled) {

            return withTraceContext().flatMapMany(it -> Flux
                    .from(new RedisPublisher<>(decorate(commandSupplier, it), connection, dissolve, getScheduler().next())));
        }

        return Flux.from(new RedisPublisher<>(commandSupplier, connection, dissolve, getScheduler().next()));
    }

    private Mono<TraceContext> withTraceContext() {

        return Tracing.getContext()
                .switchIfEmpty(Mono.fromSupplier(() -> clientResources.tracing().initialTraceContextProvider()))
                .flatMap(TraceContextProvider::getTraceContextLater).defaultIfEmpty(TraceContext.EMPTY);
    }

    protected <T> Mono<T> createMono(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return createMono(() -> new Command<>(type, output, args));
    }

    public <T> Mono<T> createMono(Supplier<RedisCommand<K, V, T>> commandSupplier) {

        if (tracingEnabled) {

            return withTraceContext().flatMap(it -> Mono
                    .from(new RedisPublisher<>(decorate(commandSupplier, it), connection, false, getScheduler().next())));
        }

        return Mono.from(new RedisPublisher<>(commandSupplier, connection, false, getScheduler().next()));
    }

    private <T> Supplier<RedisCommand<K, V, T>> decorate(Supplier<RedisCommand<K, V, T>> commandSupplier,
            TraceContext traceContext) {
        return () -> new TracedCommand<>(commandSupplier.get(), traceContext);
    }

    @Override
    public Mono<Long> dbsize() {
        return createMono(commandBuilder::dbsize);
    }

    @Override
    public Mono<String> debugCrashAndRecover(Long delay) {
        return createMono(() -> (commandBuilder.debugCrashAndRecover(delay)));
    }

    @Override
    public Mono<String> debugHtstats(int db) {
        return createMono(() -> commandBuilder.debugHtstats(db));
    }

    @Override
    public Mono<String> debugObject(K key) {
        return createMono(() -> commandBuilder.debugObject(key));
    }

    @Override
    public Mono<Void> debugOom() {
        return createMono(commandBuilder::debugOom).then();
    }

    @Override
    public Mono<String> debugReload() {
        return createMono(() -> (commandBuilder.debugReload()));
    }

    @Override
    public Mono<String> debugRestart(Long delay) {
        return createMono(() -> (commandBuilder.debugRestart(delay)));
    }

    @Override
    public Mono<String> debugSdslen(K key) {
        return createMono(() -> (commandBuilder.debugSdslen(key)));
    }

    @Override
    public Mono<Void> debugSegfault() {
        return createFlux(commandBuilder::debugSegfault).then();
    }

    @Override
    public Mono<Long> decr(K key) {
        return createMono(() -> commandBuilder.decr(key));
    }

    @Override
    public Mono<Long> decrby(K key, long amount) {
        return createMono(() -> commandBuilder.decrby(key, amount));
    }

    @Override
    public Mono<Long> del(K... keys) {
        return createMono(() -> commandBuilder.del(keys));
    }

    public Mono<Long> del(Iterable<K> keys) {
        return createMono(() -> commandBuilder.del(keys));
    }

    @Override
    public String digest(String script) {
        return digest(encodeScript(script));
    }

    @Override
    public String digest(byte[] script) {
        return Base16.digest(script);
    }

    @Override
    public Mono<String> discard() {
        return createMono(commandBuilder::discard);
    }

    @SuppressWarnings("unchecked")
    public <T> Flux<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return (Flux) createFlux(() -> new Command<>(type, output));
    }

    @SuppressWarnings("unchecked")
    public <T> Flux<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return (Flux) createFlux(() -> new Command<>(type, output, args));
    }

    @Override
    public Mono<byte[]> dump(K key) {
        return createMono(() -> commandBuilder.dump(key));
    }

    @Override
    public Mono<V> echo(V msg) {
        return createMono(() -> commandBuilder.echo(msg));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> eval(String script, ScriptOutputType type, K... keys) {
        return eval(encodeScript(script), type, keys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> eval(byte[] script, ScriptOutputType type, K... keys) {
        return (Flux<T>) createFlux(() -> commandBuilder.eval(script, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        return eval(encodeScript(script), type, keys, values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> eval(byte[] script, ScriptOutputType type, K[] keys, V... values) {
        return (Flux<T>) createFlux(() -> commandBuilder.eval(script, type, keys, values));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> evalsha(String digest, ScriptOutputType type, K... keys) {
        return (Flux<T>) createFlux(() -> commandBuilder.evalsha(digest, type, keys));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flux<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        return (Flux<T>) createFlux(() -> commandBuilder.evalsha(digest, type, keys, values));
    }

    @Override
    public Mono<TransactionResult> exec() {
        return createMono(EXEC, null, null);
    }

    public Mono<Boolean> exists(K key) {
        return createMono(() -> commandBuilder.exists(key));
    }

    @Override
    public Mono<Long> exists(K... keys) {
        return createMono(() -> commandBuilder.exists(keys));
    }

    public Mono<Long> exists(Iterable<K> keys) {
        return createMono(() -> commandBuilder.exists(keys));
    }

    @Override
    public Mono<Boolean> expire(K key, long seconds) {
        return createMono(() -> commandBuilder.expire(key, seconds));
    }

    @Override
    public Mono<Boolean> expireat(K key, long timestamp) {
        return createMono(() -> commandBuilder.expireat(key, timestamp));
    }

    @Override
    public Mono<Boolean> expireat(K key, Date timestamp) {
        return expireat(key, timestamp.getTime() / 1000);
    }

    @Override
    public void flushCommands() {
        connection.flushCommands();
    }

    @Override
    public Mono<String> flushall() {
        return createMono(commandBuilder::flushall);
    }

    @Override
    public Mono<String> flushallAsync() {
        return createMono(commandBuilder::flushallAsync);
    }

    @Override
    public Mono<String> flushdb() {
        return createMono(commandBuilder::flushdb);
    }

    @Override
    public Mono<String> flushdbAsync() {
        return createMono(commandBuilder::flushdbAsync);
    }

    @Override
    public Mono<Long> geoadd(K key, double longitude, double latitude, V member) {
        return createMono(() -> commandBuilder.geoadd(key, longitude, latitude, member));
    }

    @Override
    public Mono<Long> geoadd(K key, Object... lngLatMember) {
        return createMono(() -> commandBuilder.geoadd(key, lngLatMember));
    }

    @Override
    public Mono<Double> geodist(K key, V from, V to, Unit unit) {
        return createMono(() -> commandBuilder.geodist(key, from, to, unit));
    }

    @Override
    public Flux<Value<String>> geohash(K key, V... members) {
        return createDissolvingFlux(() -> commandBuilder.geohash(key, members));
    }

    @Override
    public Flux<Value<GeoCoordinates>> geopos(K key, V... members) {
        return createDissolvingFlux(() -> commandBuilder.geoposValues(key, members));
    }

    @Override
    public Flux<V> georadius(K key, double longitude, double latitude, double distance, Unit unit) {
        return createDissolvingFlux(() -> commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance, unit.name()));
    }

    @Override
    public Flux<GeoWithin<V>> georadius(K key, double longitude, double latitude, double distance, Unit unit, GeoArgs geoArgs) {
        return createDissolvingFlux(
                () -> commandBuilder.georadius(GEORADIUS, key, longitude, latitude, distance, unit.name(), geoArgs));
    }

    @Override
    public Mono<Long> georadius(K key, double longitude, double latitude, double distance, Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createMono(() -> commandBuilder.georadius(key, longitude, latitude, distance, unit.name(), geoRadiusStoreArgs));
    }

    protected Flux<V> georadius_ro(K key, double longitude, double latitude, double distance, Unit unit) {
        return createDissolvingFlux(
                () -> commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance, unit.name()));
    }

    protected Flux<GeoWithin<V>> georadius_ro(K key, double longitude, double latitude, double distance, Unit unit,
            GeoArgs geoArgs) {
        return createDissolvingFlux(
                () -> commandBuilder.georadius(GEORADIUS_RO, key, longitude, latitude, distance, unit.name(), geoArgs));
    }

    @Override
    public Flux<V> georadiusbymember(K key, V member, double distance, Unit unit) {
        return createDissolvingFlux(
                () -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance, unit.name()));
    }

    @Override
    public Flux<GeoWithin<V>> georadiusbymember(K key, V member, double distance, Unit unit, GeoArgs geoArgs) {
        return createDissolvingFlux(
                () -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER, key, member, distance, unit.name(), geoArgs));
    }

    @Override
    public Mono<Long> georadiusbymember(K key, V member, double distance, Unit unit, GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return createMono(() -> commandBuilder.georadiusbymember(key, member, distance, unit.name(), geoRadiusStoreArgs));
    }

    protected Flux<V> georadiusbymember_ro(K key, V member, double distance, Unit unit) {
        return createDissolvingFlux(
                () -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance, unit.name()));
    }

    protected Flux<GeoWithin<V>> georadiusbymember_ro(K key, V member, double distance, Unit unit, GeoArgs geoArgs) {
        return createDissolvingFlux(
                () -> commandBuilder.georadiusbymember(GEORADIUSBYMEMBER_RO, key, member, distance, unit.name(), geoArgs));
    }

    @Override
    public Mono<V> get(K key) {
        return createMono(() -> commandBuilder.get(key));
    }

    public StatefulConnection<K, V> getConnection() {
        return connection;
    }

    @Override
    public Mono<Long> getbit(K key, long offset) {
        return createMono(() -> commandBuilder.getbit(key, offset));
    }

    @Override
    public Mono<V> getrange(K key, long start, long end) {
        return createMono(() -> commandBuilder.getrange(key, start, end));
    }

    @Override
    public Mono<V> getset(K key, V value) {
        return createMono(() -> commandBuilder.getset(key, value));
    }

    @Override
    public Mono<Long> hdel(K key, K... fields) {
        return createMono(() -> commandBuilder.hdel(key, fields));
    }

    @Override
    public Mono<Boolean> hexists(K key, K field) {
        return createMono(() -> commandBuilder.hexists(key, field));
    }

    @Override
    public Mono<V> hget(K key, K field) {
        return createMono(() -> commandBuilder.hget(key, field));
    }

    @Override
    public Mono<Map<K, V>> hgetall(K key) {
        return createMono(() -> commandBuilder.hgetall(key));
    }

    @Override
    public Mono<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        return createMono(() -> commandBuilder.hgetall(channel, key));
    }

    @Override
    public Mono<Long> hincrby(K key, K field, long amount) {
        return createMono(() -> commandBuilder.hincrby(key, field, amount));
    }

    @Override
    public Mono<Double> hincrbyfloat(K key, K field, double amount) {
        return createMono(() -> commandBuilder.hincrbyfloat(key, field, amount));
    }

    @Override
    public Flux<K> hkeys(K key) {
        return createDissolvingFlux(() -> commandBuilder.hkeys(key));
    }

    @Override
    public Mono<Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        return createMono(() -> commandBuilder.hkeys(channel, key));
    }

    @Override
    public Mono<Long> hlen(K key) {
        return createMono(() -> commandBuilder.hlen(key));
    }

    @Override
    public Flux<KeyValue<K, V>> hmget(K key, K... fields) {
        return createDissolvingFlux(() -> commandBuilder.hmgetKeyValue(key, fields));
    }

    @Override
    public Mono<Long> hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        return createMono(() -> commandBuilder.hmget(channel, key, fields));
    }

    @Override
    public Mono<String> hmset(K key, Map<K, V> map) {
        return createMono(() -> commandBuilder.hmset(key, map));
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key) {
        return createMono(() -> commandBuilder.hscan(key));
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.hscan(key, scanArgs));
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.hscan(key, scanCursor, scanArgs));
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.hscan(key, scanCursor));
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return createMono(() -> commandBuilder.hscanStreaming(channel, key));
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.hscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.hscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.hscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Mono<Boolean> hset(K key, K field, V value) {
        return createMono(() -> commandBuilder.hset(key, field, value));
    }

    @Override
    public Mono<Long> hset(K key, Map<K, V> map) {
        return createMono(() -> commandBuilder.hset(key, map));
    }

    @Override
    public Mono<Boolean> hsetnx(K key, K field, V value) {
        return createMono(() -> commandBuilder.hsetnx(key, field, value));
    }

    @Override
    public Mono<Long> hstrlen(K key, K field) {
        return createMono(() -> commandBuilder.hstrlen(key, field));
    }

    @Override
    public Flux<V> hvals(K key) {
        return createDissolvingFlux(() -> commandBuilder.hvals(key));
    }

    @Override
    public Mono<Long> hvals(ValueStreamingChannel<V> channel, K key) {
        return createMono(() -> commandBuilder.hvals(channel, key));
    }

    @Override
    public Mono<Long> incr(K key) {
        return createMono(() -> commandBuilder.incr(key));
    }

    @Override
    public Mono<Long> incrby(K key, long amount) {
        return createMono(() -> commandBuilder.incrby(key, amount));
    }

    @Override
    public Mono<Double> incrbyfloat(K key, double amount) {
        return createMono(() -> commandBuilder.incrbyfloat(key, amount));
    }

    @Override
    public Mono<String> info() {
        return createMono(commandBuilder::info);
    }

    @Override
    public Mono<String> info(String section) {
        return createMono(() -> commandBuilder.info(section));
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public Flux<K> keys(K pattern) {
        return createDissolvingFlux(() -> commandBuilder.keys(pattern));
    }

    @Override
    public Mono<Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        return createMono(() -> commandBuilder.keys(channel, pattern));
    }

    @Override
    public Mono<Date> lastsave() {
        return createMono(commandBuilder::lastsave);
    }

    @Override
    public Mono<V> lindex(K key, long index) {
        return createMono(() -> commandBuilder.lindex(key, index));
    }

    @Override
    public Mono<Long> linsert(K key, boolean before, V pivot, V value) {
        return createMono(() -> commandBuilder.linsert(key, before, pivot, value));
    }

    @Override
    public Mono<Long> llen(K key) {
        return createMono(() -> commandBuilder.llen(key));
    }

    @Override
    public Mono<V> lpop(K key) {
        return createMono(() -> commandBuilder.lpop(key));
    }

    @Override
    public Mono<Long> lpos(K key, V value) {
        return lpos(key, value, null);
    }

    @Override
    public Mono<Long> lpos(K key, V value, LPosArgs args) {
        return createMono(() -> commandBuilder.lpos(key, value, args));
    }

    @Override
    public Flux<Long> lpos(K key, V value, int count) {
        return lpos(key, value, count, null);
    }

    @Override
    public Flux<Long> lpos(K key, V value, int count, LPosArgs args) {
        return createDissolvingFlux(() -> commandBuilder.lpos(key, value, count, args));
    }

    @Override
    public Mono<Long> lpush(K key, V... values) {
        return createMono(() -> commandBuilder.lpush(key, values));
    }

    @Override
    public Mono<Long> lpushx(K key, V... values) {
        return createMono(() -> commandBuilder.lpushx(key, values));
    }

    @Override
    public Flux<V> lrange(K key, long start, long stop) {
        return createDissolvingFlux(() -> commandBuilder.lrange(key, start, stop));
    }

    @Override
    public Mono<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createMono(() -> commandBuilder.lrange(channel, key, start, stop));
    }

    @Override
    public Mono<Long> lrem(K key, long count, V value) {
        return createMono(() -> commandBuilder.lrem(key, count, value));
    }

    @Override
    public Mono<String> lset(K key, long index, V value) {
        return createMono(() -> commandBuilder.lset(key, index, value));
    }

    @Override
    public Mono<String> ltrim(K key, long start, long stop) {
        return createMono(() -> commandBuilder.ltrim(key, start, stop));
    }

    @Override
    public Mono<Long> memoryUsage(K key) {
        return createMono(() -> commandBuilder.memoryUsage(key));
    }

    @Override
    public Flux<KeyValue<K, V>> mget(K... keys) {
        return createDissolvingFlux(() -> commandBuilder.mgetKeyValue(keys));
    }

    public Flux<KeyValue<K, V>> mget(Iterable<K> keys) {
        return createDissolvingFlux(() -> commandBuilder.mgetKeyValue(keys));
    }

    @Override
    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return createMono(() -> commandBuilder.mget(channel, keys));
    }

    public Mono<Long> mget(ValueStreamingChannel<V> channel, Iterable<K> keys) {
        return createMono(() -> commandBuilder.mget(channel, keys));
    }

    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        return createMono(() -> commandBuilder.mget(channel, keys));
    }

    @Override
    public Mono<String> migrate(String host, int port, K key, int db, long timeout) {
        return createMono(() -> commandBuilder.migrate(host, port, key, db, timeout));
    }

    @Override
    public Mono<String> migrate(String host, int port, int db, long timeout, MigrateArgs<K> migrateArgs) {
        return createMono(() -> commandBuilder.migrate(host, port, db, timeout, migrateArgs));
    }

    @Override
    public Mono<Boolean> move(K key, int db) {
        return createMono(() -> commandBuilder.move(key, db));
    }

    @Override
    public Mono<String> mset(Map<K, V> map) {
        return createMono(() -> commandBuilder.mset(map));
    }

    @Override
    public Mono<Boolean> msetnx(Map<K, V> map) {
        return createMono(() -> commandBuilder.msetnx(map));
    }

    @Override
    public Mono<String> multi() {
        return createMono(commandBuilder::multi);
    }

    @Override
    public Mono<String> objectEncoding(K key) {
        return createMono(() -> commandBuilder.objectEncoding(key));
    }

    @Override
    public Mono<Long> objectIdletime(K key) {
        return createMono(() -> commandBuilder.objectIdletime(key));
    }

    @Override
    public Mono<Long> objectRefcount(K key) {
        return createMono(() -> commandBuilder.objectRefcount(key));
    }

    @Override
    public Mono<Boolean> persist(K key) {
        return createMono(() -> commandBuilder.persist(key));
    }

    @Override
    public Mono<Boolean> pexpire(K key, long milliseconds) {
        return createMono(() -> commandBuilder.pexpire(key, milliseconds));
    }

    @Override
    public Mono<Boolean> pexpireat(K key, Date timestamp) {
        return pexpireat(key, timestamp.getTime());
    }

    @Override
    public Mono<Boolean> pexpireat(K key, long timestamp) {
        return createMono(() -> commandBuilder.pexpireat(key, timestamp));
    }

    @Override
    public Mono<Long> pfadd(K key, V... values) {
        return createMono(() -> commandBuilder.pfadd(key, values));
    }

    public Mono<Long> pfadd(K key, V value, V... values) {
        return createMono(() -> commandBuilder.pfadd(key, value, values));
    }

    @Override
    public Mono<Long> pfcount(K... keys) {
        return createMono(() -> commandBuilder.pfcount(keys));
    }

    public Mono<Long> pfcount(K key, K... keys) {
        return createMono(() -> commandBuilder.pfcount(key, keys));
    }

    @Override
    public Mono<String> pfmerge(K destkey, K... sourcekeys) {
        return createMono(() -> commandBuilder.pfmerge(destkey, sourcekeys));
    }

    public Mono<String> pfmerge(K destkey, K sourceKey, K... sourcekeys) {
        return createMono(() -> commandBuilder.pfmerge(destkey, sourceKey, sourcekeys));
    }

    @Override
    public Mono<String> ping() {
        return createMono(commandBuilder::ping);
    }

    @Override
    public Mono<String> psetex(K key, long milliseconds, V value) {
        return createMono(() -> commandBuilder.psetex(key, milliseconds, value));
    }

    @Override
    public Mono<Long> pttl(K key) {
        return createMono(() -> commandBuilder.pttl(key));
    }

    @Override
    public Mono<Long> publish(K channel, V message) {
        return createMono(() -> commandBuilder.publish(channel, message));
    }

    @Override
    public Flux<K> pubsubChannels() {
        return createDissolvingFlux(commandBuilder::pubsubChannels);
    }

    @Override
    public Flux<K> pubsubChannels(K channel) {
        return createDissolvingFlux(() -> commandBuilder.pubsubChannels(channel));
    }

    @Override
    public Mono<Long> pubsubNumpat() {
        return createMono(commandBuilder::pubsubNumpat);
    }

    @Override
    public Mono<Map<K, Long>> pubsubNumsub(K... channels) {
        return createMono(() -> commandBuilder.pubsubNumsub(channels));
    }

    @Override
    public Mono<String> quit() {
        return createMono(commandBuilder::quit);
    }

    @Override
    public Mono<K> randomkey() {
        return createMono(commandBuilder::randomkey);
    }

    @Override
    public Mono<String> readOnly() {
        return createMono(commandBuilder::readOnly);
    }

    @Override
    public Mono<String> readWrite() {
        return createMono(commandBuilder::readWrite);
    }

    @Override
    public Mono<String> rename(K key, K newKey) {
        return createMono(() -> commandBuilder.rename(key, newKey));
    }

    @Override
    public Mono<Boolean> renamenx(K key, K newKey) {
        return createMono(() -> commandBuilder.renamenx(key, newKey));
    }

    @Override
    public void reset() {
        getConnection().reset();
    }

    @Override
    public Mono<String> restore(K key, long ttl, byte[] value) {
        return createMono(() -> commandBuilder.restore(key, value, RestoreArgs.Builder.ttl(ttl)));
    }

    @Override
    public Mono<String> restore(K key, byte[] value, RestoreArgs args) {
        return createMono(() -> commandBuilder.restore(key, value, args));
    }

    @Override
    public Flux<Object> role() {
        return createDissolvingFlux(commandBuilder::role);
    }

    @Override
    public Mono<V> rpop(K key) {
        return createMono(() -> commandBuilder.rpop(key));
    }

    @Override
    public Mono<V> rpoplpush(K source, K destination) {
        return createMono(() -> commandBuilder.rpoplpush(source, destination));
    }

    @Override
    public Mono<Long> rpush(K key, V... values) {
        return createMono(() -> commandBuilder.rpush(key, values));
    }

    @Override
    public Mono<Long> rpushx(K key, V... values) {
        return createMono(() -> commandBuilder.rpushx(key, values));
    }

    @Override
    public Mono<Long> sadd(K key, V... members) {
        return createMono(() -> commandBuilder.sadd(key, members));
    }

    @Override
    public Mono<String> save() {
        return createMono(commandBuilder::save);
    }

    @Override
    public Mono<KeyScanCursor<K>> scan() {
        return createMono(commandBuilder::scan);
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.scan(scanArgs));
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.scan(scanCursor, scanArgs));
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.scan(scanCursor));
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return createMono(() -> commandBuilder.scanStreaming(channel));
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.scanStreaming(channel, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.scanStreaming(channel, scanCursor, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.scanStreaming(channel, scanCursor));
    }

    @Override
    public Mono<Long> scard(K key) {
        return createMono(() -> commandBuilder.scard(key));
    }

    @Override
    public Flux<Boolean> scriptExists(String... digests) {
        return createDissolvingFlux(() -> commandBuilder.scriptExists(digests));
    }

    @Override
    public Mono<String> scriptFlush() {
        return createMono(commandBuilder::scriptFlush);
    }

    @Override
    public Mono<String> scriptKill() {
        return createMono(commandBuilder::scriptKill);
    }

    @Override
    public Mono<String> scriptLoad(String script) {
        return scriptLoad(encodeScript(script));
    }

    @Override
    public Mono<String> scriptLoad(byte[] script) {
        return createMono(() -> commandBuilder.scriptLoad(script));
    }

    @Override
    public Flux<V> sdiff(K... keys) {
        return createDissolvingFlux(() -> commandBuilder.sdiff(keys));
    }

    @Override
    public Mono<Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        return createMono(() -> commandBuilder.sdiff(channel, keys));
    }

    @Override
    public Mono<Long> sdiffstore(K destination, K... keys) {
        return createMono(() -> commandBuilder.sdiffstore(destination, keys));
    }

    public Mono<String> select(int db) {
        return createMono(() -> commandBuilder.select(db));
    }

    @Override
    public Mono<String> set(K key, V value) {
        return createMono(() -> commandBuilder.set(key, value));
    }

    @Override
    public Mono<String> set(K key, V value, SetArgs setArgs) {
        return createMono(() -> commandBuilder.set(key, value, setArgs));
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        connection.setAutoFlushCommands(autoFlush);
    }

    public void setTimeout(Duration timeout) {
        connection.setTimeout(timeout);
    }

    @Override
    public Mono<Long> setbit(K key, long offset, int value) {
        return createMono(() -> commandBuilder.setbit(key, offset, value));
    }

    @Override
    public Mono<String> setex(K key, long seconds, V value) {
        return createMono(() -> commandBuilder.setex(key, seconds, value));
    }

    @Override
    public Mono<Boolean> setnx(K key, V value) {
        return createMono(() -> commandBuilder.setnx(key, value));
    }

    @Override
    public Mono<Long> setrange(K key, long offset, V value) {
        return createMono(() -> commandBuilder.setrange(key, offset, value));
    }

    @Override
    public Mono<Void> shutdown(boolean save) {
        return createMono(() -> commandBuilder.shutdown(save)).then();
    }

    @Override
    public Flux<V> sinter(K... keys) {
        return createDissolvingFlux(() -> commandBuilder.sinter(keys));
    }

    @Override
    public Mono<Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        return createMono(() -> commandBuilder.sinter(channel, keys));
    }

    @Override
    public Mono<Long> sinterstore(K destination, K... keys) {
        return createMono(() -> commandBuilder.sinterstore(destination, keys));
    }

    @Override
    public Mono<Boolean> sismember(K key, V member) {
        return createMono(() -> commandBuilder.sismember(key, member));
    }

    @Override
    public Mono<String> slaveof(String host, int port) {
        return createMono(() -> commandBuilder.slaveof(host, port));
    }

    @Override
    public Mono<String> slaveofNoOne() {
        return createMono(() -> commandBuilder.slaveofNoOne());
    }

    @Override
    public Flux<Object> slowlogGet() {
        return createDissolvingFlux(() -> commandBuilder.slowlogGet());
    }

    @Override
    public Flux<Object> slowlogGet(int count) {
        return createDissolvingFlux(() -> commandBuilder.slowlogGet(count));
    }

    @Override
    public Mono<Long> slowlogLen() {
        return createMono(() -> commandBuilder.slowlogLen());
    }

    @Override
    public Mono<String> slowlogReset() {
        return createMono(() -> commandBuilder.slowlogReset());
    }

    @Override
    public Flux<V> smembers(K key) {
        return createDissolvingFlux(() -> commandBuilder.smembers(key));
    }

    @Override
    public Mono<Long> smembers(ValueStreamingChannel<V> channel, K key) {
        return createMono(() -> commandBuilder.smembers(channel, key));
    }

    @Override
    public Mono<Boolean> smove(K source, K destination, V member) {
        return createMono(() -> commandBuilder.smove(source, destination, member));
    }

    @Override
    public Flux<V> sort(K key) {
        return createDissolvingFlux(() -> commandBuilder.sort(key));
    }

    @Override
    public Mono<Long> sort(ValueStreamingChannel<V> channel, K key) {
        return createMono(() -> commandBuilder.sort(channel, key));
    }

    @Override
    public Flux<V> sort(K key, SortArgs sortArgs) {
        return createDissolvingFlux(() -> commandBuilder.sort(key, sortArgs));
    }

    @Override
    public Mono<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        return createMono(() -> commandBuilder.sort(channel, key, sortArgs));
    }

    @Override
    public Mono<Long> sortStore(K key, SortArgs sortArgs, K destination) {
        return createMono(() -> commandBuilder.sortStore(key, sortArgs, destination));
    }

    @Override
    public Mono<V> spop(K key) {
        return createMono(() -> commandBuilder.spop(key));
    }

    @Override
    public Flux<V> spop(K key, long count) {
        return createDissolvingFlux(() -> commandBuilder.spop(key, count));
    }

    @Override
    public Mono<V> srandmember(K key) {
        return createMono(() -> commandBuilder.srandmember(key));
    }

    @Override
    public Flux<V> srandmember(K key, long count) {
        return createDissolvingFlux(() -> commandBuilder.srandmember(key, count));
    }

    @Override
    public Mono<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        return createMono(() -> commandBuilder.srandmember(channel, key, count));
    }

    @Override
    public Mono<Long> srem(K key, V... members) {
        return createMono(() -> commandBuilder.srem(key, members));
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key) {
        return createMono(() -> commandBuilder.sscan(key));
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.sscan(key, scanArgs));
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.sscan(key, scanCursor, scanArgs));
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.sscan(key, scanCursor));
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return createMono(() -> commandBuilder.sscanStreaming(channel, key));
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.sscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.sscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.sscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Mono<Long> strlen(K key) {
        return createMono(() -> commandBuilder.strlen(key));
    }

    @Override
    public Mono<StringMatchResult> stralgoLcs(StrAlgoArgs strAlgoArgs) {
        return createMono(() -> commandBuilder.stralgoLcs(strAlgoArgs));
    }

    @Override
    public Flux<V> sunion(K... keys) {
        return createDissolvingFlux(() -> commandBuilder.sunion(keys));
    }

    @Override
    public Mono<Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        return createMono(() -> commandBuilder.sunion(channel, keys));
    }

    @Override
    public Mono<Long> sunionstore(K destination, K... keys) {
        return createMono(() -> commandBuilder.sunionstore(destination, keys));
    }

    public Mono<String> swapdb(int db1, int db2) {
        return createMono(() -> commandBuilder.swapdb(db1, db2));
    }

    @Override
    public Flux<V> time() {
        return createDissolvingFlux(commandBuilder::time);
    }

    @Override
    public Mono<Long> touch(K... keys) {
        return createMono(() -> commandBuilder.touch(keys));
    }

    public Mono<Long> touch(Iterable<K> keys) {
        return createMono(() -> commandBuilder.touch(keys));
    }

    @Override
    public Mono<Long> ttl(K key) {
        return createMono(() -> commandBuilder.ttl(key));
    }

    @Override
    public Mono<String> type(K key) {
        return createMono(() -> commandBuilder.type(key));
    }

    @Override
    public Mono<Long> unlink(K... keys) {
        return createMono(() -> commandBuilder.unlink(keys));
    }

    public Mono<Long> unlink(Iterable<K> keys) {
        return createMono(() -> commandBuilder.unlink(keys));
    }

    @Override
    public Mono<String> unwatch() {
        return createMono(commandBuilder::unwatch);
    }

    @Override
    public Mono<Long> waitForReplication(int replicas, long timeout) {
        return createMono(() -> commandBuilder.wait(replicas, timeout));
    }

    @Override
    public Mono<String> watch(K... keys) {
        return createMono(() -> commandBuilder.watch(keys));
    }

    @Override
    public Mono<Long> xack(K key, K group, String... messageIds) {
        return createMono(() -> commandBuilder.xack(key, group, messageIds));
    }

    @Override
    public Mono<String> xadd(K key, Map<K, V> body) {
        return createMono(() -> commandBuilder.xadd(key, null, body));
    }

    @Override
    public Mono<String> xadd(K key, XAddArgs args, Map<K, V> body) {
        return createMono(() -> commandBuilder.xadd(key, args, body));
    }

    @Override
    public Mono<String> xadd(K key, Object... keysAndValues) {
        return createMono(() -> commandBuilder.xadd(key, null, keysAndValues));
    }

    @Override
    public Mono<String> xadd(K key, XAddArgs args, Object... keysAndValues) {
        return createMono(() -> commandBuilder.xadd(key, args, keysAndValues));
    }

    @Override
    public Flux<StreamMessage<K, V>> xclaim(K key, Consumer<K> consumer, long minIdleTime, String... messageIds) {
        return createDissolvingFlux(
                () -> commandBuilder.xclaim(key, consumer, XClaimArgs.Builder.minIdleTime(minIdleTime), messageIds));
    }

    @Override
    public Flux<StreamMessage<K, V>> xclaim(K key, Consumer<K> consumer, XClaimArgs args, String... messageIds) {
        return createDissolvingFlux(() -> commandBuilder.xclaim(key, consumer, args, messageIds));
    }

    @Override
    public Mono<Long> xdel(K key, String... messageIds) {
        return createMono(() -> commandBuilder.xdel(key, messageIds));
    }

    @Override
    public Mono<String> xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K group) {
        return createMono(() -> commandBuilder.xgroupCreate(streamOffset, group, null));
    }

    @Override
    public Mono<String> xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K group, XGroupCreateArgs args) {
        return createMono(() -> commandBuilder.xgroupCreate(streamOffset, group, args));
    }

    @Override
    public Mono<Long> xgroupDelconsumer(K key, Consumer<K> consumer) {
        return createMono(() -> commandBuilder.xgroupDelconsumer(key, consumer));
    }

    @Override
    public Mono<Boolean> xgroupDestroy(K key, K group) {
        return createMono(() -> commandBuilder.xgroupDestroy(key, group));
    }

    @Override
    public Mono<String> xgroupSetid(XReadArgs.StreamOffset<K> streamOffset, K group) {
        return createMono(() -> commandBuilder.xgroupSetid(streamOffset, group));
    }

    @Override
    public Flux<Object> xinfoStream(K key) {
        return createDissolvingFlux(() -> commandBuilder.xinfoStream(key));
    }

    @Override
    public Flux<Object> xinfoGroups(K key) {
        return createDissolvingFlux(() -> commandBuilder.xinfoGroups(key));
    }

    @Override
    public Flux<Object> xinfoConsumers(K key, K group) {
        return createDissolvingFlux(() -> commandBuilder.xinfoConsumers(key, group));
    }

    @Override
    public Mono<Long> xlen(K key) {
        return createMono(() -> commandBuilder.xlen(key));
    }

    @Override
    public Mono<PendingMessages> xpending(K key, K group) {
        return createMono(() -> commandBuilder.xpending(key, group));
    }

    @Override
    public Flux<PendingMessage> xpending(K key, K group, Range<String> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.xpending(key, group, range, limit));
    }

    @Override
    public Flux<PendingMessage> xpending(K key, Consumer<K> consumer, Range<String> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.xpending(key, consumer, range, limit));
    }

    @Override
    public Flux<StreamMessage<K, V>> xrange(K key, Range<String> range) {
        return createDissolvingFlux(() -> commandBuilder.xrange(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<StreamMessage<K, V>> xrange(K key, Range<String> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.xrange(key, range, limit));
    }

    @Override
    public Flux<StreamMessage<K, V>> xread(XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingFlux(() -> commandBuilder.xread(null, streams));
    }

    @Override
    public Flux<StreamMessage<K, V>> xread(XReadArgs args, XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingFlux(() -> commandBuilder.xread(args, streams));
    }

    @Override
    public Flux<StreamMessage<K, V>> xreadgroup(Consumer<K> consumer, XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingFlux(() -> commandBuilder.xreadgroup(consumer, null, streams));
    }

    @Override
    public Flux<StreamMessage<K, V>> xreadgroup(Consumer<K> consumer, XReadArgs args, XReadArgs.StreamOffset<K>... streams) {
        return createDissolvingFlux(() -> commandBuilder.xreadgroup(consumer, args, streams));
    }

    @Override
    public Flux<StreamMessage<K, V>> xrevrange(K key, Range<String> range) {
        return xrevrange(key, range, Limit.unlimited());
    }

    @Override
    public Flux<StreamMessage<K, V>> xrevrange(K key, Range<String> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.xrevrange(key, range, limit));
    }

    @Override
    public Mono<Long> xtrim(K key, long count) {
        return xtrim(key, false, count);
    }

    @Override
    public Mono<Long> xtrim(K key, boolean approximateTrimming, long count) {
        return createMono(() -> commandBuilder.xtrim(key, approximateTrimming, count));
    }

    @Override
    public Mono<KeyValue<K, ScoredValue<V>>> bzpopmin(long timeout, K... keys) {
        return createMono(() -> commandBuilder.bzpopmin(timeout, keys));
    }

    @Override
    public Mono<KeyValue<K, ScoredValue<V>>> bzpopmax(long timeout, K... keys) {
        return createMono(() -> commandBuilder.bzpopmax(timeout, keys));
    }

    @Override
    public Mono<Long> zadd(K key, double score, V member) {
        return createMono(() -> commandBuilder.zadd(key, null, score, member));
    }

    @Override
    public Mono<Long> zadd(K key, Object... scoresAndValues) {
        return createMono(() -> commandBuilder.zadd(key, null, scoresAndValues));
    }

    @Override
    public Mono<Long> zadd(K key, ScoredValue<V>... scoredValues) {
        return createMono(() -> commandBuilder.zadd(key, null, (Object[]) scoredValues));
    }

    @Override
    public Mono<Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        return createMono(() -> commandBuilder.zadd(key, zAddArgs, score, member));
    }

    @Override
    public Mono<Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        return createMono(() -> commandBuilder.zadd(key, zAddArgs, scoresAndValues));
    }

    @Override
    public Mono<Long> zadd(K key, ZAddArgs zAddArgs, ScoredValue<V>... scoredValues) {
        return createMono(() -> commandBuilder.zadd(key, zAddArgs, (Object[]) scoredValues));
    }

    @Override
    public Mono<Double> zaddincr(K key, double score, V member) {
        return createMono(() -> commandBuilder.zaddincr(key, null, score, member));
    }

    @Override
    public Mono<Double> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        return createMono(() -> commandBuilder.zaddincr(key, zAddArgs, score, member));
    }

    @Override
    public Mono<Long> zcard(K key) {
        return createMono(() -> commandBuilder.zcard(key));
    }

    public Mono<Long> zcount(K key, double min, double max) {
        return createMono(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Mono<Long> zcount(K key, String min, String max) {
        return createMono(() -> commandBuilder.zcount(key, min, max));
    }

    @Override
    public Mono<Long> zcount(K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zcount(key, range));
    }

    @Override
    public Mono<Double> zincrby(K key, double amount, V member) {
        return createMono(() -> commandBuilder.zincrby(key, amount, member));
    }

    @Override
    public Mono<Long> zinterstore(K destination, K... keys) {
        return createMono(() -> commandBuilder.zinterstore(destination, keys));
    }

    @Override
    public Mono<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createMono(() -> commandBuilder.zinterstore(destination, storeArgs, keys));
    }

    @Override
    public Mono<Long> zlexcount(K key, String min, String max) {
        return createMono(() -> commandBuilder.zlexcount(key, min, max));
    }

    @Override
    public Mono<Long> zlexcount(K key, Range<? extends V> range) {
        return createMono(() -> commandBuilder.zlexcount(key, range));
    }

    @Override
    public Mono<ScoredValue<V>> zpopmin(K key) {
        return createMono(() -> commandBuilder.zpopmin(key));
    }

    @Override
    public Flux<ScoredValue<V>> zpopmin(K key, long count) {
        return createDissolvingFlux(() -> commandBuilder.zpopmin(key, count));
    }

    @Override
    public Mono<ScoredValue<V>> zpopmax(K key) {
        return createMono(() -> commandBuilder.zpopmax(key));
    }

    @Override
    public Flux<ScoredValue<V>> zpopmax(K key, long count) {
        return createDissolvingFlux(() -> commandBuilder.zpopmax(key, count));
    }

    @Override
    public Flux<V> zrange(K key, long start, long stop) {
        return createDissolvingFlux(() -> commandBuilder.zrange(key, start, stop));
    }

    @Override
    public Mono<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createMono(() -> commandBuilder.zrange(channel, key, start, stop));
    }

    @Override
    public Flux<ScoredValue<V>> zrangeWithScores(K key, long start, long stop) {
        return createDissolvingFlux(() -> commandBuilder.zrangeWithScores(key, start, stop));
    }

    @Override
    public Mono<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createMono(() -> commandBuilder.zrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Flux<V> zrangebylex(K key, String min, String max) {
        return createDissolvingFlux(() -> commandBuilder.zrangebylex(key, min, max));
    }

    @Override
    public Flux<V> zrangebylex(K key, Range<? extends V> range) {
        return createDissolvingFlux(() -> commandBuilder.zrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<V> zrangebylex(K key, String min, String max, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrangebylex(key, min, max, offset, count));
    }

    @Override
    public Flux<V> zrangebylex(K key, Range<? extends V> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrangebylex(key, range, limit));
    }

    @Override
    public Flux<V> zrangebyscore(K key, double min, double max) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public Flux<V> zrangebyscore(K key, String min, String max) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, min, max));
    }

    @Override
    public Flux<V> zrangebyscore(K key, double min, double max, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public Flux<V> zrangebyscore(K key, String min, String max, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, min, max, offset, count));
    }

    @Override
    public Flux<V> zrangebyscore(K key, Range<? extends Number> range) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<V> zrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscore(key, range, limit));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, min, max));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, min, max, offset, count));
    }

    @Override
    public Mono<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
        return createMono(() -> commandBuilder.zrangebyscore(channel, key, range, limit));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, min, max));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, min, max, offset, count));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<ScoredValue<V>> zrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, min, max, offset, count));
    }

    @Override
    public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        return createMono(() -> commandBuilder.zrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public Mono<Long> zrank(K key, V member) {
        return createMono(() -> commandBuilder.zrank(key, member));
    }

    @Override
    public Mono<Long> zrem(K key, V... members) {
        return createMono(() -> commandBuilder.zrem(key, members));
    }

    @Override
    public Mono<Long> zremrangebylex(K key, String min, String max) {
        return createMono(() -> commandBuilder.zremrangebylex(key, min, max));
    }

    @Override
    public Mono<Long> zremrangebylex(K key, Range<? extends V> range) {
        return createMono(() -> commandBuilder.zremrangebylex(key, range));
    }

    @Override
    public Mono<Long> zremrangebyrank(K key, long start, long stop) {
        return createMono(() -> commandBuilder.zremrangebyrank(key, start, stop));
    }

    @Override
    public Mono<Long> zremrangebyscore(K key, double min, double max) {
        return createMono(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Mono<Long> zremrangebyscore(K key, String min, String max) {
        return createMono(() -> commandBuilder.zremrangebyscore(key, min, max));
    }

    @Override
    public Mono<Long> zremrangebyscore(K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zremrangebyscore(key, range));
    }

    @Override
    public Flux<V> zrevrange(K key, long start, long stop) {
        return createDissolvingFlux(() -> commandBuilder.zrevrange(key, start, stop));
    }

    @Override
    public Mono<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createMono(() -> commandBuilder.zrevrange(channel, key, start, stop));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangeWithScores(key, start, stop));
    }

    @Override
    public Mono<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        return createMono(() -> commandBuilder.zrevrangeWithScores(channel, key, start, stop));
    }

    @Override
    public Flux<V> zrevrangebylex(K key, Range<? extends V> range) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebylex(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<V> zrevrangebylex(K key, Range<? extends V> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebylex(key, range, limit));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, double max, double min) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, String max, String min) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, max, min));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, Range<? extends Number> range) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, max, min, offset, count));
    }

    @Override
    public Flux<V> zrevrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscore(key, range, limit));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, max, min));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, max, min, offset, count));
    }

    @Override
    public Mono<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
        return createMono(() -> commandBuilder.zrevrangebyscore(channel, key, range, limit));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, range, Limit.unlimited()));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, max, min, offset, count));
    }

    @Override
    public Flux<ScoredValue<V>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        return createDissolvingFlux(() -> commandBuilder.zrevrangebyscoreWithScores(key, range, limit));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, range, Limit.unlimited()));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, max, min, offset, count));
    }

    @Override
    public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        return createMono(() -> commandBuilder.zrevrangebyscoreWithScores(channel, key, range, limit));
    }

    @Override
    public Mono<Long> zrevrank(K key, V member) {
        return createMono(() -> commandBuilder.zrevrank(key, member));
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key) {
        return createMono(() -> commandBuilder.zscan(key));
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.zscan(key, scanArgs));
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.zscan(key, scanCursor, scanArgs));
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.zscan(key, scanCursor));
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return createMono(() -> commandBuilder.zscanStreaming(channel, key));
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.zscanStreaming(channel, key, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return createMono(() -> commandBuilder.zscanStreaming(channel, key, scanCursor, scanArgs));
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return createMono(() -> commandBuilder.zscanStreaming(channel, key, scanCursor));
    }

    @Override
    public Mono<Double> zscore(K key, V member) {
        return createMono(() -> commandBuilder.zscore(key, member));
    }

    @Override
    public Mono<Long> zunionstore(K destination, K... keys) {
        return createMono(() -> commandBuilder.zunionstore(destination, keys));
    }

    @Override
    public Mono<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        return createMono(() -> commandBuilder.zunionstore(destination, storeArgs, keys));
    }

    private byte[] encodeScript(String script) {
        LettuceAssert.notNull(script, "Lua script must not be null");
        LettuceAssert.notEmpty(script, "Lua script must not be empty");
        return script.getBytes(getConnection().getOptions().getScriptCharset());
    }

}
