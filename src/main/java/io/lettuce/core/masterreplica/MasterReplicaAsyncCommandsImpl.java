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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.PipelinedRedisFuture;
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
 * Master/Replica variant of {@link RedisAsyncCommandsImpl} that gives the scan command family node-affine routing.
 * <p>
 * Scan cursors are node-local state: a continuation must return to the node that served the initial request. On a
 * Master/Replica connection {@code ReadFrom} otherwise load-balances every read across nodes, so a continuation could land on a
 * different replica and interpret the cursor against its own keyspace, yielding undefined results (issue #3287).
 * <p>
 * Mirroring {@code RedisAdvancedClusterAsyncCommandsImpl}, each scan overload delegates to {@link #masterReplicaScan} which
 * runs the command on a single node connection and wraps the result into a source-aware cursor subtype (see
 * {@link MasterReplicaScanSupport}). The generic {@link MasterReplicaChannelWriter} is left unaware of scan semantics.
 * {@link MasterReplicaReactiveCommandsImpl} is the reactive counterpart.
 *
 * @author Sanghun Lee
 */
public class MasterReplicaAsyncCommandsImpl<K, V> extends RedisAsyncCommandsImpl<K, V> {

    MasterReplicaAsyncCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Orchestration
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Run a scan on a single node and wrap the result so continuations stick to it.
     * <ul>
     * <li>In a transaction, run on the master (transaction) connection without pinning or wrapping.</li>
     * <li>Continuation (cursor carries a source): pin to the issuing node, failing fast if it left the topology.</li>
     * <li>Initial request: select a read node via {@code ReadFrom} and stamp it onto the returned cursor.</li>
     * </ul>
     */
    private <T extends ScanCursor> RedisFuture<T> masterReplicaScan(ScanCursor cursor,
            BiFunction<RedisAsyncCommands<K, V>, ScanCursor, RedisFuture<T>> scanFunction, SourceScanCursorFactory<T> factory) {

        MasterReplicaConnectionProvider<K, V> provider = provider();

        if (connection().getChannelWriter().isInTransaction()) {
            CompletableFuture<StatefulRedisConnection<K, V>> master = provider.getConnectionAsync(ConnectionIntent.WRITE);
            return new PipelinedRedisFuture<>(run(master, scanFunction, cursor));
        }

        SourceAwareScanCursor source = MasterReplicaScanSupport.asSourceAware(cursor);

        if (source != null) {
            CompletableFuture<StatefulRedisConnection<K, V>> pinned = provider.getPinnedConnectionAsync(source.getSource(),
                    source.getSourceRole());
            CompletionStage<T> stage = run(pinned, scanFunction, cursor);
            return new PipelinedRedisFuture<>(
                    stage.thenApply(result -> factory.create(source.getSource(), source.getSourceRole(), result)));
        }

        AtomicReference<RedisNodeDescription> selected = new AtomicReference<>();
        CompletableFuture<StatefulRedisConnection<K, V>> read = provider.getConnectionAsync(ConnectionIntent.READ,
                selected::set);
        CompletionStage<T> stage = run(read, scanFunction, cursor);
        return new PipelinedRedisFuture<>(
                stage.thenApply(result -> factory.create(selected.get().getUri(), selected.get().getRole(), result)));
    }

    private <T extends ScanCursor> CompletionStage<T> run(CompletableFuture<StatefulRedisConnection<K, V>> connectionFuture,
            BiFunction<RedisAsyncCommands<K, V>, ScanCursor, RedisFuture<T>> scanFunction, ScanCursor cursor) {
        return connectionFuture
                .thenCompose(nodeConnection -> scanFunction.apply(nodeConnection.async(), cursor).toCompletableFuture());
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
    public RedisFuture<KeyScanCursor<K>> scan() {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(), MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(cursor, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, RedisAsyncCommands::scan, MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(channel),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.scan(channel, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(channel, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.scan(channel, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // HSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(key),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(key, scanArgs),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(key, cursor),
                MasterReplicaScanSupport.mapScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // HSCAN NOVALUES
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public RedisFuture<KeyScanCursor<K>> hscanNovalues(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(key),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> hscanNovalues(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(key, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(key, cursor, scanArgs),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(key, cursor),
                MasterReplicaScanSupport.keyScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.hscanNovalues(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.hscanNovalues(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // SSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(key),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(key, scanArgs),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(key, cursor),
                MasterReplicaScanSupport.valueScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.sscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.sscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    // ------------------------------------------------------------------------------------------------------------------
    // ZSCAN
    // ------------------------------------------------------------------------------------------------------------------

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(key),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(key, scanArgs),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(key, cursor, scanArgs),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(key, cursor),
                MasterReplicaScanSupport.scoredValueScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(channel, key),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        return masterReplicaScan(ScanCursor.INITIAL, (c, cursor) -> c.zscan(channel, key, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(channel, key, cursor, scanArgs),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

    @Override
    public RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        return masterReplicaScan(scanCursor, (c, cursor) -> c.zscan(channel, key, cursor),
                MasterReplicaScanSupport.streamScanCursorFactory());
    }

}
