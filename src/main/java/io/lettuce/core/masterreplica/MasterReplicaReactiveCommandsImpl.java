/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed to Redis, Inc. under one or more contributor license agreements.
 */
package io.lettuce.core.masterreplica;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.masterreplica.MasterReplicaScanSupport.SourceAwareScanCursor;
import io.lettuce.core.masterreplica.MasterReplicaScanSupport.SourceScanCursorFactory;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;
import io.lettuce.core.protocol.ConnectionIntent;

/**
 * Master/Replica variant of {@link RedisReactiveCommandsImpl} that gives the scan command family node-affine routing, the
 * reactive counterpart of {@link MasterReplicaAsyncCommandsImpl}. Node selection is deferred to subscription time so
 * re-subscribing a scan {@link Mono} re-resolves the target node. See
 * <a href="https://github.com/redis/lettuce/issues/3287">#3287</a>.
 *
 * @author Sanghun Lee
 */
public class MasterReplicaReactiveCommandsImpl<K, V> extends RedisReactiveCommandsImpl<K, V> {

    MasterReplicaReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Orchestration
    // ------------------------------------------------------------------------------------------------------------------

    private <T extends ScanCursor> Mono<T> masterReplicaScan(ScanCursor cursor,
            BiFunction<RedisReactiveCommands<K, V>, ScanCursor, Mono<T>> scanFunction, SourceScanCursorFactory<T> factory) {

        return Mono.defer(() -> {

            MasterReplicaConnectionProvider<K, V> provider = provider();

            if (connection().getChannelWriter().isInTransaction()) {
                return Mono.fromCompletionStage(provider.getConnectionAsync(ConnectionIntent.WRITE))
                        .flatMap(nodeConnection -> scanFunction.apply(nodeConnection.reactive(), cursor));
            }

            SourceAwareScanCursor source = MasterReplicaScanSupport.asSourceAware(cursor);

            if (source != null) {
                return Mono.fromCompletionStage(provider.getPinnedConnectionAsync(source.getSource(), source.getSourceRole()))
                        .flatMap(nodeConnection -> scanFunction.apply(nodeConnection.reactive(), cursor))
                        .map(result -> factory.create(source.getSource(), source.getSourceRole(), result));
            }

            AtomicReference<RedisNodeDescription> selected = new AtomicReference<>();
            return Mono.fromCompletionStage(provider.getConnectionAsync(ConnectionIntent.READ, selected::set))
                    .flatMap(nodeConnection -> scanFunction.apply(nodeConnection.reactive(), cursor))
                    .map(result -> factory.create(selected.get().getUri(), selected.get().getRole(), result));
        });
    }

    @SuppressWarnings("unchecked")
    private StatefulRedisMasterReplicaConnectionImpl<K, V> connection() {
        return (StatefulRedisMasterReplicaConnectionImpl<K, V>) getConnection();
    }

    @SuppressWarnings("unchecked")
    private MasterReplicaConnectionProvider<K, V> provider() {
        return (MasterReplicaConnectionProvider<K, V>) connection().getChannelWriter().getUpstreamReplicaConnectionProvider();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // SCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public Mono<KeyScanCursor<K>> scan() {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(), MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(cursor, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, RedisReactiveCommands::scan, MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(channel),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(channel, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(channel, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(channel, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // HSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(key),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(key, scanArgs),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public Mono<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(key, cursor),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // HSCAN NOVALUES
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public Mono<KeyScanCursor<K>> hscanNovalues(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(key),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> hscanNovalues(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(key, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(key, cursor, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(key, cursor),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // SSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(key),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(key, scanArgs),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(key, cursor),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // ZSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(key),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(key, scanArgs),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public Mono<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(key, cursor),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

}
