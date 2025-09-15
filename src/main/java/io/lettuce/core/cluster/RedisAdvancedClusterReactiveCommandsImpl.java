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
package io.lettuce.core.cluster;

import static io.lettuce.core.cluster.ClusterScanSupport.*;
import static io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.lettuce.core.json.JsonParser;
import org.reactivestreams.Publisher;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisServerReactiveCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.protocol.ConnectionIntent;
import reactor.core.publisher.Flux;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.AggregationReply.Cursor;

import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.ExplainArgs;

import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * An advanced reactive and thread-safe API to a Redis Cluster connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Jon Chambers
 * @since 4.0
 */
public class RedisAdvancedClusterReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisAdvancedClusterReactiveCommands<K, V> {

    private static final Predicate<RedisClusterNode> ALL_NODES = node -> true;

    private final RedisCodec<K, V> codec;

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection.
     * @param codec Codec used to encode/decode keys and values.
     * @param parser the implementation of the {@link JsonParser} to use
     * @deprecated since 5.2, use
     *             {@link #RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnection, RedisCodec, Supplier)}.
     */
    @Deprecated
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection.
     * @param codec Codec used to encode/decode keys and values.
     * @deprecated since 5.2, use
     *             {@link #RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnection, RedisCodec, Supplier)}.
     */
    @Deprecated
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnectionImpl<K, V> connection,
            RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection.
     * @param codec Codec used to encode/decode keys and values.
     * @param parser the implementation of the {@link JsonParser} to use
     */
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
        this.codec = codec;
    }

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection.
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterReactiveCommandsImpl(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.codec = codec;
    }

    @Override
    public Mono<String> clientSetname(K name) {

        List<Publisher<String>> publishers = new ArrayList<>();

        publishers.add(super.clientSetname(name));

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            Mono<StatefulRedisConnection<K, V>> byNodeId = getStatefulConnection(redisClusterNode.getNodeId());

            publishers.add(byNodeId.flatMap(conn -> {

                if (conn.isOpen()) {
                    return conn.reactive().clientSetname(name);
                }
                return Mono.empty();
            }));

            Mono<StatefulRedisConnection<K, V>> byHost = getStatefulConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            publishers.add(byHost.flatMap(conn -> {

                if (conn.isOpen()) {
                    return conn.reactive().clientSetname(name);
                }
                return Mono.empty();
            }));
        }

        return Flux.merge(publishers).last();
    }

    @Override
    public Mono<Long> clusterCountKeysInSlot(int slot) {

        Mono<RedisClusterReactiveCommands<K, V>> connectionBySlot = findConnectionBySlotReactive(slot);
        return connectionBySlot.flatMap(cmd -> cmd.clusterCountKeysInSlot(slot));
    }

    @Override
    public Flux<K> clusterGetKeysInSlot(int slot, int count) {

        Mono<RedisClusterReactiveCommands<K, V>> connectionBySlot = findConnectionBySlotReactive(slot);
        return connectionBySlot.flatMapMany(conn -> conn.clusterGetKeysInSlot(slot, count));
    }

    @Override
    public Mono<Long> dbsize() {

        Map<String, Publisher<Long>> publishers = executeOnUpstream(RedisServerReactiveCommands::dbsize);
        return Flux.merge(publishers.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Long> del(K... keys) {
        return del(Arrays.asList(keys));
    }

    @Override
    public Mono<Long> del(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.del(keys);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.del(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Long> exists(K... keys) {
        return exists(Arrays.asList(keys));
    }

    public Mono<Long> exists(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.exists(keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.exists(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<String> flushall() {

        Map<String, Publisher<String>> publishers = executeOnUpstream(RedisServerReactiveCommands::flushall);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> flushall(FlushMode flushMode) {

        Map<String, Publisher<String>> publishers = executeOnUpstream(it -> it.flushall(flushMode));
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> flushallAsync() {

        Map<String, Publisher<String>> publishers = executeOnUpstream(RedisServerReactiveCommands::flushallAsync);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> flushdb() {

        Map<String, Publisher<String>> publishers = executeOnUpstream(RedisServerReactiveCommands::flushdb);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> flushdb(FlushMode flushMode) {

        Map<String, Publisher<String>> publishers = executeOnUpstream(it -> it.flushdb(flushMode));
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Flux<K> keys(K pattern) {

        Map<String, Publisher<K>> publishers = executeOnUpstream(commands -> commands.keys(pattern));
        return Flux.merge(publishers.values());
    }

    @Override
    public Mono<Long> keys(KeyStreamingChannel<K> channel, K pattern) {

        Map<String, Publisher<Long>> publishers = executeOnUpstream(commands -> commands.keys(channel, pattern));
        return Flux.merge(publishers.values()).reduce((accu, next) -> accu + next);
    }

    @Override
    public Flux<KeyValue<K, V>> mget(K... keys) {
        return mget(Arrays.asList(keys));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Flux<KeyValue<K, V>> mget(Iterable<K> keys) {
        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(keyList);
        }

        List<Publisher<KeyValue<K, V>>> publishers = partitioned.values().stream().map(super::mget)
                .collect(Collectors.toList());

        return Flux.mergeSequential(publishers).collectList().map(results -> {
            KeyValue<K, V>[] values = new KeyValue[keyList.size()];
            int offset = 0;

            for (List<K> partitionKeys : partitioned.values()) {
                for (int i = 0; i < keyList.size(); i++) {
                    int index = partitionKeys.indexOf(keyList.get(i));
                    if (index != -1) {
                        values[i] = results.get(offset + index);
                    }
                }
                offset += partitionKeys.size();
            }

            return Arrays.asList(values);
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        return mget(channel, Arrays.asList(keys));
    }

    @Override
    public Mono<Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.mget(channel, keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.mget(channel, entry.getValue()));
        }

        return Flux.merge(publishers).reduce(Long::sum);
    }

    @Override
    public Mono<Boolean> msetnx(Map<K, V> map) {

        return pipeliningWithMap(map, kvMap -> RedisAdvancedClusterReactiveCommandsImpl.super.msetnx(kvMap).flux(),
                booleanFlux -> booleanFlux).reduce((accu, next) -> accu && next);
    }

    @Override
    public Mono<String> mset(Map<K, V> map) {
        return pipeliningWithMap(map, kvMap -> RedisAdvancedClusterReactiveCommandsImpl.super.mset(kvMap).flux(),
                booleanFlux -> booleanFlux).last();
    }

    @Override
    public Mono<K> randomkey() {

        Partitions partitions = getStatefulConnection().getPartitions();

        if (partitions.isEmpty()) {
            return super.randomkey();
        }

        int index = ThreadLocalRandom.current().nextInt(partitions.size());

        Mono<RedisClusterReactiveCommands<K, V>> connection = getConnectionReactive(partitions.getPartition(index).getNodeId());
        return connection.flatMap(RedisKeyReactiveCommands::randomkey);
    }

    @Override
    public Mono<String> scriptFlush() {
        Map<String, Publisher<String>> publishers = executeOnNodes(RedisScriptingReactiveCommands::scriptFlush, ALL_NODES);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<String> scriptKill() {
        Map<String, Publisher<String>> publishers = executeOnNodes(RedisScriptingReactiveCommands::scriptKill, ALL_NODES);
        return Flux.merge(publishers.values()).onErrorReturn("OK").last();
    }

    @Override
    public Mono<String> scriptLoad(byte[] script) {
        Map<String, Publisher<String>> publishers = executeOnNodes((commands) -> commands.scriptLoad(script), ALL_NODES);
        return Flux.merge(publishers.values()).last();
    }

    @Override
    public Mono<AggregationReply<K, V>> ftAggregate(K index, V query, AggregateArgs<K, V> args) {
        return routeFirstIterationOrSuper(() -> super.ftAggregate(index, query, args),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftAggregate(index, query, args)).mapNotNull(reply -> {
                            if (reply != null) {
                                reply.getCursor().filter(c -> c.getCursorId() > 0)
                                        .ifPresent(c -> c.setNodeId(node.getNodeId()));
                            }
                            return reply;
                        }));
    }

    // --- Keyless RediSearch commands: route to an arbitrary upstream (master) ---
    // --- Read selection honoring ReadFrom (for read-only Search commands) ---

    private RedisClusterNode randomUpstreamNode() {
        Partitions partitions = getStatefulConnection().getPartitions();
        List<RedisClusterNode> masters = new ArrayList<>();
        for (RedisClusterNode n : partitions) {
            if (n.is(UPSTREAM)) {
                masters.add(n);
            }
        }
        if (masters.isEmpty()) {
            return null;
        }
        int idx = ThreadLocalRandom.current().nextInt(masters.size());
        return masters.get(idx);
    }

    private <R> Mono<R> routeFirstIterationOrSuper(Supplier<Mono<R>> superCall,
            Function<RedisClusterNode, Mono<R>> routedCall) {
        ReadFrom rf = getStatefulConnection().getReadFrom();
        if (rf != null && rf != ReadFrom.UPSTREAM) {
            return superCall.get();
        }
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return superCall.get();
        }
        return routedCall.apply(node);
    }

    private <R> Flux<R> routeFirstIterationOrSuperMany(Supplier<Flux<R>> superCall,
            Function<RedisClusterNode, Flux<R>> routedCall) {
        ReadFrom rf = getStatefulConnection().getReadFrom();
        if (rf != null && rf != ReadFrom.UPSTREAM) {
            return superCall.get();
        }
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return superCall.get();
        }
        return routedCall.apply(node);
    }

    @Override
    public Mono<SearchReply<K, V>> ftSearch(K index, V query, SearchArgs<K, V> args) {
        return routeFirstIterationOrSuper(() -> super.ftSearch(index, query, args),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftSearch(index, query, args)));
    }

    @Override
    public Mono<SearchReply<K, V>> ftSearch(K index, V query) {
        return ftSearch(index, query, SearchArgs.<K, V> builder().build());
    }

    @Override
    public Mono<String> ftExplain(K index, V query) {
        return routeFirstIterationOrSuper(() -> super.ftExplain(index, query),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftExplain(index, query)));
    }

    @Override
    public Mono<String> ftExplain(K index, V query, ExplainArgs<K, V> args) {
        return routeFirstIterationOrSuper(() -> super.ftExplain(index, query, args),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftExplain(index, query, args)));
    }

    @Override
    public Flux<V> ftTagvals(K index, K fieldName) {
        return routeFirstIterationOrSuperMany(() -> super.ftTagvals(index, fieldName),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMapMany(conn -> conn.ftTagvals(index, fieldName)));
    }

    @Override
    public Mono<SpellCheckResult<V>> ftSpellcheck(K index, V query) {
        return routeFirstIterationOrSuper(() -> super.ftSpellcheck(index, query),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftSpellcheck(index, query)));
    }

    @Override
    public Mono<SpellCheckResult<V>> ftSpellcheck(K index, V query,
            io.lettuce.core.search.arguments.SpellCheckArgs<K, V> args) {
        return routeFirstIterationOrSuper(() -> super.ftSpellcheck(index, query, args),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMap(conn -> conn.ftSpellcheck(index, query, args)));
    }

    @Override
    public Mono<Long> ftDictadd(K dict, V... terms) {
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return super.ftDictadd(dict, terms);
        }
        return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                .flatMap(conn -> conn.ftDictadd(dict, terms));
    }

    @Override
    public Mono<Long> ftDictdel(K dict, V... terms) {
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return super.ftDictdel(dict, terms);
        }
        return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                .flatMap(conn -> conn.ftDictdel(dict, terms));
    }

    @Override
    public Flux<V> ftDictdump(K dict) {
        return routeFirstIterationOrSuperMany(() -> super.ftDictdump(dict),
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMapMany(conn -> conn.ftDictdump(dict)));
    }

    @Override
    public Mono<String> ftAliasadd(K alias, K index) {
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return super.ftAliasadd(alias, index);
        }
        return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                .flatMap(conn -> conn.ftAliasadd(alias, index));
    }

    @Override
    public Mono<String> ftAliasupdate(K alias, K index) {
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return super.ftAliasupdate(alias, index);
        }
        return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                .flatMap(conn -> conn.ftAliasupdate(alias, index));
    }

    @Override
    public Mono<String> ftAliasdel(K alias) {
        RedisClusterNode node = randomUpstreamNode();
        if (node == null) {
            return super.ftAliasdel(alias);
        }
        return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort()).flatMap(conn -> conn.ftAliasdel(alias));
    }

    @Override
    public Flux<V> ftList() {
        return routeFirstIterationOrSuperMany(super::ftList,
                node -> getConnectionReactive(node.getUri().getHost(), node.getUri().getPort())
                        .flatMapMany(conn -> conn.ftList()));
    }

    @Override
    public Mono<AggregationReply<K, V>> ftAggregate(K index, V query) {
        return ftAggregate(index, query, null);
    }

    @Override
    public Mono<Void> shutdown(boolean save) {
        Map<String, Publisher<Void>> publishers = executeOnNodes(commands -> commands.shutdown(save), ALL_NODES);
        return Flux.merge(publishers.values()).then();
    }

    @Override
    public Mono<AggregationReply<K, V>> ftCursorread(K index, Cursor cursor, int count) {
        if (cursor == null) {
            return Mono.error(new IllegalArgumentException("cursor must not be null"));
        }
        long cursorId = cursor.getCursorId();
        if (cursorId <= 0) {
            return Mono.just(new AggregationReply<>());
        }
        Optional<String> nodeIdOpt = cursor.getNodeId();
        if (!nodeIdOpt.isPresent()) {
            return Mono.error(new IllegalArgumentException("Cursor missing nodeId; cannot route cursor READ in cluster mode"));
        }
        String nodeId = nodeIdOpt.get();
        StatefulRedisConnection<K, V> byNode = getStatefulConnection().getConnection(nodeId, ConnectionIntent.WRITE);
        return byNode.reactive().ftCursorread(index, cursor, count).map(reply -> {
            if (reply != null) {
                reply.getCursor().ifPresent(c -> c.setNodeId(nodeId));
            }
            return reply;
        });
    }

    @Override
    public Mono<AggregationReply<K, V>> ftCursorread(K index, Cursor cursor) {
        return ftCursorread(index, cursor, -1);
    }

    @Override
    public Mono<String> ftCursordel(K index, Cursor cursor) {
        if (cursor == null) {
            return Mono.error(new IllegalArgumentException("cursor must not be null"));
        }
        long cursorId = cursor.getCursorId();
        if (cursorId <= 0) {
            return Mono.just("OK");
        }
        Optional<String> nodeIdOpt = cursor.getNodeId();
        if (!nodeIdOpt.isPresent()) {
            return Mono.error(new IllegalArgumentException("Cursor missing nodeId; cannot route cursor DEL in cluster mode"));
        }
        String nodeId = nodeIdOpt.get();
        StatefulRedisConnection<K, V> byNode = getStatefulConnection().getConnection(nodeId, ConnectionIntent.WRITE);
        return byNode.reactive().ftCursordel(index, cursor);
    }

    @Override
    public Mono<Long> touch(K... keys) {
        return touch(Arrays.asList(keys));
    }

    public Mono<Long> touch(Iterable<K> keys) {

        List<K> keyList = LettuceLists.newList(keys);
        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keyList);

        if (partitioned.size() < 2) {
            return super.touch(keyList);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.touch(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public Mono<Long> unlink(K... keys) {
        return unlink(Arrays.asList(keys));
    }

    @Override
    public Mono<Long> unlink(Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, keys);

        if (partitioned.size() < 2) {
            return super.unlink(keys);
        }

        List<Publisher<Long>> publishers = new ArrayList<>();

        for (Map.Entry<Integer, List<K>> entry : partitioned.entrySet()) {
            publishers.add(super.unlink(entry.getValue()));
        }

        return Flux.merge(publishers).reduce((accu, next) -> accu + next);
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).reactive();
    }

    private Mono<StatefulRedisConnection<K, V>> getStatefulConnection(String nodeId) {
        return getMono(getConnectionProvider().getConnectionAsync(ConnectionIntent.WRITE, nodeId));
    }

    private Mono<RedisClusterReactiveCommands<K, V>> getConnectionReactive(String nodeId) {
        return getMono(getConnectionProvider().<K, V> getConnectionAsync(ConnectionIntent.WRITE, nodeId))
                .map(StatefulRedisConnection::reactive);
    }

    @Override
    public RedisClusterReactiveCommands<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).reactive();
    }

    private Mono<RedisClusterReactiveCommands<K, V>> getConnectionReactive(String host, int port) {
        return getMono(getConnectionProvider().<K, V> getConnectionAsync(ConnectionIntent.WRITE, host, port))
                .map(StatefulRedisConnection::reactive);
    }

    private Mono<StatefulRedisConnection<K, V>> getStatefulConnection(String host, int port) {
        return getMono(getConnectionProvider().<K, V> getConnectionAsync(ConnectionIntent.WRITE, host, port));
    }

    @Override
    public StatefulRedisClusterConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnection<K, V>) super.getConnection();
    }

    @Override
    public Mono<KeyScanCursor<K>> scan() {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(), reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(cursor, scanArgs),
                reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return clusterScan(scanCursor, RedisKeyReactiveCommands::scan, reactiveClusterKeyScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return clusterScan(ScanCursor.INITIAL, (connection, cursor) -> connection.scan(channel, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor, scanArgs),
                reactiveClusterStreamScanCursorMapper());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return clusterScan(scanCursor, (connection, cursor) -> connection.scan(channel, cursor),
                reactiveClusterStreamScanCursorMapper());
    }

    @SuppressWarnings("unchecked")
    private <T extends ScanCursor> Mono<T> clusterScan(ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Mono<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Mono<T>> resultMapper) {

        return clusterScan(getStatefulConnection(), getConnectionProvider(), cursor, scanFunction,
                (ClusterScanSupport.ScanCursorMapper) resultMapper);
    }

    private <T> Flux<T> pipeliningWithMap(Map<K, V> map, Function<Map<K, V>, Flux<T>> function,
            Function<Flux<T>, Flux<T>> resultFunction) {

        Map<Integer, List<K>> partitioned = SlotHash.partition(codec, map.keySet());

        if (partitioned.size() < 2) {
            return function.apply(map);
        }

        List<Flux<T>> publishers = partitioned.values().stream().map(ks -> {
            Map<K, V> op = new HashMap<>();
            ks.forEach(k -> op.put(k, map.get(k)));
            return function.apply(op);
        }).collect(Collectors.toList());

        return resultFunction.apply(Flux.merge(publishers));
    }

    /**
     * Run a command on all available masters,
     *
     * @param function function producing the command
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, Publisher<T>> executeOnUpstream(
            Function<RedisClusterReactiveCommands<K, V>, ? extends Publisher<T>> function) {
        return executeOnNodes(function, redisClusterNode -> redisClusterNode.is(UPSTREAM));
    }

    /**
     * Run a command on all available nodes that match {@code filter}.
     *
     * @param function function producing the command
     * @param filter filter function for the node selection
     * @param <T> result type
     * @return map of a key (counter) and commands.
     */
    protected <T> Map<String, Publisher<T>> executeOnNodes(
            Function<RedisClusterReactiveCommands<K, V>, ? extends Publisher<T>> function, Predicate<RedisClusterNode> filter) {

        Map<String, Publisher<T>> executions = new HashMap<>();

        for (RedisClusterNode redisClusterNode : getStatefulConnection().getPartitions()) {

            if (!filter.test(redisClusterNode)) {
                continue;
            }

            RedisURI uri = redisClusterNode.getUri();
            Mono<RedisClusterReactiveCommands<K, V>> connection = getConnectionReactive(uri.getHost(), uri.getPort());

            executions.put(redisClusterNode.getNodeId(), connection.flatMapMany(function::apply));
        }
        return executions;
    }

    private Mono<RedisClusterReactiveCommands<K, V>> findConnectionBySlotReactive(int slot) {

        RedisClusterNode node = getStatefulConnection().getPartitions().getPartitionBySlot(slot);
        if (node != null) {
            return getConnectionReactive(node.getUri().getHost(), node.getUri().getPort());
        }

        return Mono.error(new RedisException("No partition for slot " + slot));
    }

    private AsyncClusterConnectionProvider getConnectionProvider() {

        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) getStatefulConnection().getChannelWriter();
        return (AsyncClusterConnectionProvider) writer.getClusterConnectionProvider();
    }

    /**
     * Perform a SCAN in the cluster.
     *
     */
    static <T extends ScanCursor, K, V> Mono<T> clusterScan(StatefulRedisClusterConnection<K, V> connection,
            AsyncClusterConnectionProvider connectionProvider, ScanCursor cursor,
            BiFunction<RedisKeyReactiveCommands<K, V>, ScanCursor, Mono<T>> scanFunction,
            ClusterScanSupport.ScanCursorMapper<Mono<T>> mapper) {

        List<String> nodeIds = ClusterScanSupport.getNodeIds(connection, cursor);
        String currentNodeId = ClusterScanSupport.getCurrentNodeId(cursor, nodeIds);
        ScanCursor continuationCursor = ClusterScanSupport.getContinuationCursor(cursor);

        Mono<T> scanCursor = getMono(connectionProvider.<K, V> getConnectionAsync(ConnectionIntent.WRITE, currentNodeId))
                .flatMap(conn -> scanFunction.apply(conn.reactive(), continuationCursor));
        return mapper.map(nodeIds, currentNodeId, scanCursor);
    }

    private static <T> Mono<T> getMono(CompletableFuture<T> future) {
        return Mono.fromCompletionStage(future);
    }

}
